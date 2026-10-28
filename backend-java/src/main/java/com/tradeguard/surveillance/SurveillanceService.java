package com.tradeguard.surveillance;

import com.tradeguard.audit.AuditService;
import com.tradeguard.common.DuplicateResourceException;
import com.tradeguard.common.InvalidRequestException;
import com.tradeguard.common.ResourceNotFoundException;
import com.tradeguard.config.MarketProperties;
import com.tradeguard.config.SurveillanceProperties;
import com.tradeguard.instrument.Instrument;
import com.tradeguard.instrument.InstrumentRepository;
import com.tradeguard.order.OrderRepository;
import com.tradeguard.order.Trade;
import com.tradeguard.order.TradeRepository;
import com.tradeguard.order.TradingOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.util.List;

@Service
public class SurveillanceService {

    private final AlertRepository alerts;
    private final CaseRepository cases;
    private final TradeRepository trades;
    private final OrderRepository orders;
    private final InstrumentRepository instruments;
    private final TradeWindowStore windowStore;
    private final SurveillanceProperties surveillance;
    private final MarketProperties market;
    private final AuditService auditService;

    public SurveillanceService(AlertRepository alerts, CaseRepository cases,
                               TradeRepository trades, OrderRepository orders,
                               InstrumentRepository instruments, TradeWindowStore windowStore,
                               SurveillanceProperties surveillance, MarketProperties market,
                               AuditService auditService) {
        this.alerts = alerts;
        this.cases = cases;
        this.trades = trades;
        this.orders = orders;
        this.instruments = instruments;
        this.windowStore = windowStore;
        this.surveillance = surveillance;
        this.market = market;
        this.auditService = auditService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processTrade(long tradeId) {
        Trade trade = trades.findById(tradeId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到成交: " + tradeId));
        TradingOrder buy = orders.findById(trade.getBuyOrderId())
                .orElseThrow(() -> new IllegalStateException(
                        "成交買方委託不存在: " + trade.getBuyOrderId()));
        TradingOrder sell = orders.findById(trade.getSellOrderId())
                .orElseThrow(() -> new IllegalStateException(
                        "成交賣方委託不存在: " + trade.getSellOrderId()));
        Instrument instrument = instruments.findBySymbol(trade.getSymbol())
                .orElseThrow(() -> new IllegalStateException(
                        "成交商品不存在: " + trade.getSymbol()));
        List<Trade> window = windowStore.recordAndRead(
                trade.getSymbol(), trade, surveillance.getWindow());

        if (buy.getUserId().equals(sell.getUserId())) {
            recordAlert("WASH_TRADE", trade, "HIGH",
                    "同一帳戶同時出現在買方與賣方委託；此規則僅產生待人工判斷的線索。");
        }
        if (isClosingPriceAnomaly(trade, buy, instrument, window)) {
            recordAlert("CLOSING_MARKUP", trade, "HIGH",
                    "尾盤視窗內偏價買進成交達設定數量門檻；此規則僅產生待人工判斷的線索。");
        }
    }

    @Transactional(readOnly = true)
    public Page<AlertResponse> findAlerts(Pageable pageable) {
        return alerts.findAllByOrderByOccurredAtDescIdDesc(pageable)
                .map(AlertResponse::from);
    }

    @Transactional
    public CaseResponse proposeCase(long alertId, String username) {
        alerts.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到警示: " + alertId));
        if (cases.existsByAlertId(alertId)) {
            throw new DuplicateResourceException("此警示已建立案件。");
        }
        SurveillanceCase surveillanceCase =
                cases.saveAndFlush(new SurveillanceCase(alertId, username));
        auditService.record(null, username, "SURVEILLANCE_CASE_PROPOSED",
                "case_id=" + surveillanceCase.getId() + ",alert_id=" + alertId);
        return CaseResponse.from(surveillanceCase);
    }

    @Transactional(readOnly = true)
    public Page<CaseResponse> findCases(Pageable pageable) {
        return cases.findAllByOrderByCreatedAtDescIdDesc(pageable)
                .map(CaseResponse::from);
    }

    @Transactional
    public CaseResponse reviewCase(long caseId, String reviewer, ReviewCaseRequest request) {
        SurveillanceCase surveillanceCase = cases.findByIdForUpdate(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到案件: " + caseId));
        if (surveillanceCase.getProposedBy().equals(reviewer)) {
            throw new InvalidRequestException("提案人不可覆核自己的案件。");
        }
        if (surveillanceCase.getStatus() != CaseStatus.PROPOSED) {
            throw new InvalidRequestException("案件已完成覆核。");
        }
        surveillanceCase.review(reviewer, request.approve(), request.note());
        cases.flush();
        auditService.record(null, reviewer, "SURVEILLANCE_CASE_REVIEWED",
                "case_id=" + caseId + ",decision="
                        + (request.approve() ? "APPROVED" : "REJECTED"));
        return CaseResponse.from(surveillanceCase);
    }

    private boolean isClosingPriceAnomaly(Trade trade, TradingOrder buy,
                                          Instrument instrument, List<Trade> window) {
        ZoneId zone = market.getZone();
        var localTime = trade.getCreatedAt().atZone(zone).toLocalTime();
        if (localTime.isBefore(market.getClose().minusMinutes(
                surveillance.getClosingWindowMinutes()))
                || localTime.isAfter(market.getClose())) {
            return false;
        }
        BigDecimal deviationPct = trade.getPrice().subtract(instrument.getReferencePrice())
                .abs().multiply(BigDecimal.valueOf(100))
                .divide(instrument.getReferencePrice(), 4, RoundingMode.HALF_UP);
        if (deviationPct.compareTo(surveillance.getClosingPriceDeviationPct()) < 0) {
            return false;
        }
        long buyerWindowQuantity = 0;
        for (Trade windowTrade : window) {
            TradingOrder windowBuy = orders.findById(windowTrade.getBuyOrderId()).orElse(null);
            if (windowBuy != null && windowBuy.getUserId().equals(buy.getUserId())) {
                buyerWindowQuantity = Math.addExact(buyerWindowQuantity,
                        windowTrade.getQuantity());
            }
        }
        return buyerWindowQuantity >= surveillance.getClosingMinimumQuantity();
    }

    private void recordAlert(String ruleCode, Trade trade, String severity,
                             String description) {
        String key = ruleCode + ":" + trade.getId();
        if (!alerts.existsByAlertKey(key)) {
            Alert alert = alerts.saveAndFlush(new Alert(key, ruleCode, trade.getSymbol(),
                    severity, trade.getId(), description, trade.getCreatedAt()));
            auditService.record(null, "system:surveillance", "SURVEILLANCE_ALERT_CREATED",
                    "alert_id=" + alert.getId() + ",rule=" + ruleCode
                            + ",trade_id=" + trade.getId());
        }
    }
}
