package com.tradeguard.audit;

import com.tradeguard.instrument.InstrumentRepository;
import com.tradeguard.order.OrderEvent;
import com.tradeguard.order.OrderEventRepository;
import com.tradeguard.order.OrderEventType;
import com.tradeguard.order.OrderRepository;
import com.tradeguard.order.OrderStatus;
import com.tradeguard.order.Trade;
import com.tradeguard.order.TradeRepository;
import com.tradeguard.order.TradingOrder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReconciliationService {

    private final OrderRepository orders;
    private final TradeRepository trades;
    private final OrderEventRepository events;
    private final InstrumentRepository instruments;

    public ReconciliationService(OrderRepository orders, TradeRepository trades,
                                 OrderEventRepository events,
                                 InstrumentRepository instruments) {
        this.orders = orders;
        this.trades = trades;
        this.events = events;
        this.instruments = instruments;
    }

    @Transactional(readOnly = true)
    public ReconciliationReport reconcile() {
        List<TradingOrder> allOrders = orders.findAll();
        List<Trade> allTrades = trades.findAllByOrderByIdAsc();
        List<OrderEvent> allEvents = events.findAllByOrderByIdAsc();
        List<ReconciliationIssue> issues = new ArrayList<>();

        Map<OrderKey, TradingOrder> ordersByKey = new HashMap<>();
        Map<Long, TradingOrder> ordersById = new HashMap<>();
        Map<OrderKey, Long> filledByOrder = new HashMap<>();
        Map<PairKey, Long> tradeQuantityByPair = new HashMap<>();
        Map<PairKey, Long> eventQuantityByPair = new HashMap<>();
        Map<OrderKey, Long> acceptedCount = new HashMap<>();
        Map<OrderKey, Long> canceledCount = new HashMap<>();
        Map<String, Map<Long, Long>> sequenceNumbers = new HashMap<>();

        for (TradingOrder order : allOrders) {
            OrderKey key = new OrderKey(order.getSymbol(), order.getSeqNo());
            ordersByKey.put(key, order);
            ordersById.put(order.getId(), order);
            sequenceNumbers.computeIfAbsent(order.getSymbol(), ignored -> new HashMap<>())
                    .put(order.getSeqNo(), order.getSeqNo());
        }

        for (Trade trade : allTrades) {
            TradingOrder buy = ordersById.get(trade.getBuyOrderId());
            TradingOrder sell = ordersById.get(trade.getSellOrderId());
            if (buy == null || sell == null) {
                issues.add(issue("TRADE_ORDER_MISSING", trade.getSymbol(),
                        "Trade " + trade.getId() + " references a missing order."));
                continue;
            }
            if (!trade.getSymbol().equals(buy.getSymbol())
                    || !trade.getSymbol().equals(sell.getSymbol())) {
                issues.add(issue("TRADE_SYMBOL_MISMATCH", trade.getSymbol(),
                        "Trade " + trade.getId() + " and its orders use different symbols."));
            }
            filledByOrder.merge(new OrderKey(buy.getSymbol(), buy.getSeqNo()),
                    trade.getQuantity(), Math::addExact);
            filledByOrder.merge(new OrderKey(sell.getSymbol(), sell.getSeqNo()),
                    trade.getQuantity(), Math::addExact);
            PairKey pair = PairKey.of(trade.getSymbol(), buy.getSeqNo(), sell.getSeqNo());
            tradeQuantityByPair.merge(pair, trade.getQuantity(), Math::addExact);
        }

        for (OrderEvent event : allEvents) {
            OrderKey key = new OrderKey(event.getSymbol(), event.getOrderSeqNo());
            if (event.getEventType() == OrderEventType.ORDER_ACCEPTED) {
                acceptedCount.merge(key, 1L, Long::sum);
            } else if (event.getEventType() == OrderEventType.ORDER_CANCELED) {
                canceledCount.merge(key, 1L, Long::sum);
            } else if (event.getEventType() == OrderEventType.TRADE_EXECUTED) {
                if (event.getCounterpartySeqNo() == null || event.getQuantity() == null) {
                    issues.add(issue("TRADE_EVENT_INVALID", event.getSymbol(),
                            "Trade event " + event.getId() + " is missing its counterparty or quantity."));
                    continue;
                }
                PairKey pair = PairKey.of(event.getSymbol(), event.getOrderSeqNo(),
                        event.getCounterpartySeqNo());
                eventQuantityByPair.merge(pair, event.getQuantity(), Math::addExact);
            }
        }

        ordersByKey.forEach((key, order) -> {
            if (acceptedCount.getOrDefault(key, 0L) != 1L) {
                issues.add(issue("ORDER_ACCEPT_EVENT_MISMATCH", key.symbol,
                        "Order seq_no " + key.seqNo + " must have exactly one acceptance event."));
            }
            long tradedQuantity = filledByOrder.getOrDefault(key, 0L);
            if (tradedQuantity != order.getFilledQuantity()) {
                issues.add(issue("ORDER_TRADE_QUANTITY_MISMATCH", key.symbol,
                        "Order seq_no " + key.seqNo + " filled quantity differs from its trades."));
            }
            boolean canceled = canceledCount.getOrDefault(key, 0L) == 1L;
            if ((order.getStatus() == OrderStatus.CANCELED) != canceled
                    || canceledCount.getOrDefault(key, 0L) > 1L) {
                issues.add(issue("ORDER_CANCEL_EVENT_MISMATCH", key.symbol,
                        "Order seq_no " + key.seqNo + " cancellation event differs from order status."));
            }
        });

        for (OrderEvent event : allEvents) {
            if (!ordersByKey.containsKey(new OrderKey(event.getSymbol(), event.getOrderSeqNo()))) {
                issues.add(issue("EVENT_ORDER_MISSING", event.getSymbol(),
                        "Event " + event.getId() + " references an unknown order."));
            }
            if (event.getEventType() == OrderEventType.TRADE_EXECUTED
                    && event.getCounterpartySeqNo() != null
                    && !ordersByKey.containsKey(new OrderKey(event.getSymbol(),
                    event.getCounterpartySeqNo()))) {
                issues.add(issue("EVENT_COUNTERPARTY_MISSING", event.getSymbol(),
                        "Trade event " + event.getId() + " references an unknown counterparty."));
            }
        }

        comparePairQuantities(tradeQuantityByPair, eventQuantityByPair, issues);
        instruments.findAll().forEach(instrument -> {
            long lastSeqNo = instrument.getLastSeqNo();
            Map<Long, Long> symbolSeqNos = sequenceNumbers.getOrDefault(
                    instrument.getSymbol(), Map.of());
            if (symbolSeqNos.size() != lastSeqNo) {
                issues.add(issue("SEQUENCE_GAP", instrument.getSymbol(),
                        "Expected " + lastSeqNo + " orders but found "
                                + symbolSeqNos.size() + " distinct sequence numbers."));
                return;
            }
            for (long expected = 1; expected <= lastSeqNo; expected++) {
                if (!symbolSeqNos.containsKey(expected)) {
                    issues.add(issue("SEQUENCE_GAP", instrument.getSymbol(),
                            "Missing order sequence " + expected + "."));
                    break;
                }
            }
        });

        return new ReconciliationReport(issues.isEmpty(), allOrders.size(), allTrades.size(),
                allEvents.size(), List.copyOf(issues), java.time.Instant.now());
    }

    private void comparePairQuantities(Map<PairKey, Long> tradeQuantities,
                                       Map<PairKey, Long> eventQuantities,
                                       List<ReconciliationIssue> issues) {
        Map<PairKey, Long> allPairs = new HashMap<>(tradeQuantities);
        eventQuantities.forEach(allPairs::putIfAbsent);
        allPairs.forEach((pair, tradeQuantity) -> {
            long eventQuantity = eventQuantities.getOrDefault(pair, 0L);
            if (tradeQuantity.longValue() != eventQuantity) {
                issues.add(issue("TRADE_EVENT_QUANTITY_MISMATCH", pair.symbol,
                        "Orders " + pair.firstSeqNo + "/" + pair.secondSeqNo
                                + " have trade quantity " + tradeQuantity
                                + " but event quantity " + eventQuantity + "."));
            }
        });
    }

    private ReconciliationIssue issue(String code, String symbol, String message) {
        return new ReconciliationIssue(code, symbol, message);
    }

    private record OrderKey(String symbol, long seqNo) {
    }

    private record PairKey(String symbol, long firstSeqNo, long secondSeqNo) {

        private static PairKey of(String symbol, long first, long second) {
            return first < second
                    ? new PairKey(symbol, first, second)
                    : new PairKey(symbol, second, first);
        }
    }
}
