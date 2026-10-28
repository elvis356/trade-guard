package com.tradeguard.order;

import com.tradeguard.audit.AuditService;
import com.tradeguard.common.DuplicateResourceException;
import com.tradeguard.common.InvalidRequestException;
import com.tradeguard.common.ResourceNotFoundException;
import com.tradeguard.instrument.Instrument;
import com.tradeguard.instrument.InstrumentRepository;
import com.tradeguard.order.dto.CreateOrderRequest;
import com.tradeguard.order.dto.OrderResponse;
import com.tradeguard.order.dto.TradeResponse;
import com.tradeguard.surveillance.TradeCommittedEvent;
import com.tradeguard.user.AppUser;
import com.tradeguard.user.AppUserRepository;
import com.tradeguard.user.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderService {

    private static final List<OrderStatus> OPEN_STATUSES =
            List.of(OrderStatus.NEW, OrderStatus.PARTIALLY_FILLED);

    private final InstrumentRepository instruments;
    private final OrderRepository orders;
    private final TradeRepository trades;
    private final OrderEventRepository orderEvents;
    private final AppUserRepository users;
    private final OrderValidator validator;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditService auditService;

    public OrderService(InstrumentRepository instruments, OrderRepository orders,
                        TradeRepository trades, OrderEventRepository orderEvents,
                        AppUserRepository users,
                        OrderValidator validator, ApplicationEventPublisher eventPublisher,
                        AuditService auditService) {
        this.instruments = instruments;
        this.orders = orders;
        this.trades = trades;
        this.orderEvents = orderEvents;
        this.users = users;
        this.validator = validator;
        this.eventPublisher = eventPublisher;
        this.auditService = auditService;
    }

    @Transactional
    public OrderResponse create(String username, CreateOrderRequest request) {
        AppUser user = users.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user no longer exists."));
        var existing = orders.findByUserIdAndClientOrderId(user.getId(), request.clientOrderId());
        if (existing.isPresent()) {
            TradingOrder original = existing.get();
            if (!sameRequest(original, request)) {
                throw new DuplicateResourceException(
                        "client_order_id 已被不同的委託使用: " + request.clientOrderId());
            }
            return OrderResponse.from(original);
        }

        Instrument instrument = instruments.findActiveBySymbolForUpdate(request.symbol())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "找不到啟用中的商品代碼: " + request.symbol()));
        validator.validate(request, instrument);

        long seqNo = instrument.allocateSeqNo();
        orderEvents.saveAndFlush(OrderEvent.accepted(request, user.getId(), seqNo));
        TradingOrder incoming = orders.saveAndFlush(new TradingOrder(
                request.clientOrderId(), seqNo, user.getId(), request.symbol(),
                request.side(), request.price(), request.qty()));
        auditService.record(user.getId(), username, "ORDER_ACCEPTED",
                "symbol=" + request.symbol() + ",seq_no=" + seqNo
                        + ",client_order_id=" + request.clientOrderId());
        match(incoming);
        return OrderResponse.from(incoming);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> findMine(String username, Pageable pageable) {
        AppUser user = users.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user no longer exists."));
        return orders.findByUserIdOrderByIdDesc(user.getId(), pageable)
                .map(OrderResponse::from);
    }

    @Transactional
    public OrderResponse cancel(String username, long orderId) {
        AppUser user = users.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user no longer exists."));
        TradingOrder requested = orders.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("找不到委託: " + orderId));

        instruments.findBySymbolForUpdate(requested.getSymbol())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "找不到啟用中的商品代碼: " + requested.getSymbol()));
        TradingOrder order = orders.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("找不到委託: " + orderId));
        if (!OPEN_STATUSES.contains(order.getStatus()) || order.getRemainingQuantity() == 0) {
            throw new InvalidRequestException("委託已完成或已取消，無可取消數量。");
        }
        orderEvents.saveAndFlush(OrderEvent.canceled(order.getSymbol(), order.getSeqNo()));
        order.cancel();
        orders.flush();
        auditService.record(user.getId(), username, "ORDER_CANCELED",
                "order_id=" + orderId + ",symbol=" + order.getSymbol()
                        + ",seq_no=" + order.getSeqNo());
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public Page<TradeResponse> findTrades(String symbol, Pageable pageable) {
        if (!instruments.existsBySymbol(symbol)) {
            throw new ResourceNotFoundException("找不到商品代碼: " + symbol);
        }
        return trades.findBySymbolOrderByIdDesc(symbol, pageable)
                .map(TradeResponse::from);
    }

    private void match(TradingOrder incoming) {
        List<TradingOrder> restingOrders = incoming.getSide() == OrderSide.BUY
                ? orders.findBySymbolAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByLimitPriceAscSeqNoAsc(
                        incoming.getSymbol(), OrderSide.SELL, OPEN_STATUSES, 0)
                : orders.findBySymbolAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByLimitPriceDescSeqNoAsc(
                        incoming.getSymbol(), OrderSide.BUY, OPEN_STATUSES, 0);

        for (TradingOrder resting : restingOrders) {
            if (incoming.getRemainingQuantity() == 0 || !crosses(incoming, resting)) {
                break;
            }
            long fillQuantity = Math.min(incoming.getRemainingQuantity(),
                    resting.getRemainingQuantity());
            orderEvents.saveAndFlush(OrderEvent.tradeExecuted(incoming.getSymbol(),
                    incoming.getSeqNo(), resting.getSeqNo(), fillQuantity));
            incoming.fill(fillQuantity);
            resting.fill(fillQuantity);
            TradingOrder buyOrder = incoming.getSide() == OrderSide.BUY ? incoming : resting;
            TradingOrder sellOrder = incoming.getSide() == OrderSide.SELL ? incoming : resting;
            Trade trade = trades.save(new Trade(incoming.getSymbol(), buyOrder.getId(),
                    sellOrder.getId(), resting.getLimitPrice(), fillQuantity));
            auditService.record(null, "system:matching", "TRADE_EXECUTED",
                    "trade_id=" + trade.getId() + ",symbol=" + incoming.getSymbol()
                            + ",quantity=" + fillQuantity);
            eventPublisher.publishEvent(new TradeCommittedEvent(trade.getId()));
        }
        orders.flush();
        trades.flush();
    }

    private boolean crosses(TradingOrder incoming, TradingOrder resting) {
        return incoming.getSide() == OrderSide.BUY
                ? incoming.getLimitPrice().compareTo(resting.getLimitPrice()) >= 0
                : incoming.getLimitPrice().compareTo(resting.getLimitPrice()) <= 0;
    }

    private boolean sameRequest(TradingOrder existing, CreateOrderRequest request) {
        return existing.getSymbol().equals(request.symbol())
                && existing.getSide() == request.side()
                && existing.getLimitPrice().compareTo(request.price()) == 0
                && existing.getOriginalQuantity() == request.qty();
    }
}
