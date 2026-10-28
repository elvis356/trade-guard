package com.tradeguard.order;

import com.tradeguard.instrument.InstrumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderEventReplayer {

    private final OrderEventRepository events;
    private final OrderRepository orders;
    private final InstrumentRepository instruments;
    private volatile Map<String, List<RecoveredOrder>> openOrders = Map.of();

    public OrderEventReplayer(OrderEventRepository events, OrderRepository orders,
                              InstrumentRepository instruments) {
        this.events = events;
        this.orders = orders;
        this.instruments = instruments;
    }

    @Transactional(readOnly = true)
    public Map<String, List<RecoveredOrder>> replay() {
        Map<OrderKey, ReplayOrder> projection = new HashMap<>();
        for (OrderEvent event : events.findAllByOrderByIdAsc()) {
            OrderKey key = new OrderKey(event.getSymbol(), event.getOrderSeqNo());
            switch (event.getEventType()) {
                case ORDER_ACCEPTED -> accept(projection, key, event);
                case ORDER_CANCELED -> cancel(projection, key);
                case TRADE_EXECUTED -> executeTrade(projection, key, event);
            }
        }

        verifyPersistedOrders(projection);
        verifySequenceCounters(projection);
        Map<String, List<RecoveredOrder>> recovered = new LinkedHashMap<>();
        projection.values().stream()
                .filter(ReplayOrder::isOpen)
                .sorted(Comparator.comparing((ReplayOrder order) -> order.symbol)
                        .thenComparingLong(order -> order.seqNo))
                .forEach(order -> recovered.computeIfAbsent(order.symbol,
                        ignored -> new java.util.ArrayList<>()).add(order.toRecoveredOrder()));
        recovered.replaceAll((symbol, symbolOrders) -> List.copyOf(symbolOrders));
        openOrders = Map.copyOf(recovered);
        return openOrders;
    }

    public Map<String, List<RecoveredOrder>> getOpenOrders() {
        return openOrders;
    }

    private void accept(Map<OrderKey, ReplayOrder> projection, OrderKey key,
                        OrderEvent event) {
        if (event.getOrderSeqNo() < 1 || event.getUserId() == null
                || event.getClientOrderId() == null || event.getSide() == null
                || event.getLimitPrice() == null || event.getQuantity() == null
                || event.getQuantity() <= 0) {
            throw corrupt("Invalid ORDER_ACCEPTED event for " + key);
        }
        ReplayOrder order = new ReplayOrder(key.symbol, key.seqNo, event.getUserId(),
                event.getClientOrderId(), event.getSide(), event.getLimitPrice(),
                event.getQuantity());
        if (projection.putIfAbsent(key, order) != null) {
            throw corrupt("Duplicate ORDER_ACCEPTED event for " + key);
        }
    }

    private void cancel(Map<OrderKey, ReplayOrder> projection, OrderKey key) {
        ReplayOrder order = requireOrder(projection, key);
        if (!order.isOpen()) {
            throw corrupt("Cannot cancel a closed order: " + key);
        }
        order.status = OrderStatus.CANCELED;
    }

    private void executeTrade(Map<OrderKey, ReplayOrder> projection, OrderKey key,
                              OrderEvent event) {
        if (event.getCounterpartySeqNo() == null || event.getQuantity() == null
                || event.getQuantity() <= 0
                || event.getCounterpartySeqNo() == event.getOrderSeqNo()) {
            throw corrupt("Invalid TRADE_EXECUTED event for " + key);
        }
        ReplayOrder order = requireOrder(projection, key);
        ReplayOrder counterparty = requireOrder(projection,
                new OrderKey(key.symbol, event.getCounterpartySeqNo()));
        if (!order.isOpen() || !counterparty.isOpen()
                || order.side == counterparty.side
                || event.getQuantity() > order.remainingQuantity
                || event.getQuantity() > counterparty.remainingQuantity) {
            throw corrupt("Trade event violates projected order state for " + key);
        }
        order.fill(event.getQuantity());
        counterparty.fill(event.getQuantity());
    }

    private ReplayOrder requireOrder(Map<OrderKey, ReplayOrder> projection, OrderKey key) {
        ReplayOrder order = projection.get(key);
        if (order == null) {
            throw corrupt("Event references an unknown order: " + key);
        }
        return order;
    }

    private void verifyPersistedOrders(Map<OrderKey, ReplayOrder> projection) {
        List<TradingOrder> persisted = orders.findAll();
        if (persisted.size() != projection.size()) {
            throw corrupt("Event/order count mismatch: events=" + projection.size()
                    + ", orders=" + persisted.size());
        }
        for (TradingOrder order : persisted) {
            OrderKey key = new OrderKey(order.getSymbol(), order.getSeqNo());
            ReplayOrder replayed = projection.get(key);
            if (replayed == null || !replayed.userId.equals(order.getUserId())
                    || !replayed.clientOrderId.equals(order.getClientOrderId())
                    || replayed.side != order.getSide()
                    || replayed.limitPrice.compareTo(order.getLimitPrice()) != 0
                    || replayed.originalQuantity != order.getOriginalQuantity()
                    || replayed.remainingQuantity != order.getRemainingQuantity()
                    || replayed.status != order.getStatus()) {
                throw corrupt("Replayed state does not match persisted order: " + key);
            }
        }
    }

    private void verifySequenceCounters(Map<OrderKey, ReplayOrder> projection) {
        Map<String, Long> maxSeqNos = new HashMap<>();
        projection.keySet().forEach(key ->
                maxSeqNos.merge(key.symbol, key.seqNo, Math::max));
        instruments.findAll().forEach(instrument -> {
            long expected = maxSeqNos.getOrDefault(instrument.getSymbol(), 0L);
            if (instrument.getLastSeqNo() != expected) {
                throw corrupt("Sequence counter mismatch for " + instrument.getSymbol()
                        + ": counter=" + instrument.getLastSeqNo()
                        + ", events=" + expected);
            }
        });
    }

    private IllegalStateException corrupt(String message) {
        return new IllegalStateException("Order event replay failed: " + message);
    }

    public record RecoveredOrder(String symbol, long seqNo, Long userId,
                                 UUID clientOrderId, OrderSide side,
                                 BigDecimal limitPrice, long originalQuantity,
                                 long remainingQuantity, OrderStatus status) {
    }

    private record OrderKey(String symbol, long seqNo) {
    }

    private static final class ReplayOrder {
        private final String symbol;
        private final long seqNo;
        private final Long userId;
        private final UUID clientOrderId;
        private final OrderSide side;
        private final BigDecimal limitPrice;
        private final long originalQuantity;
        private long remainingQuantity;
        private OrderStatus status = OrderStatus.NEW;

        private ReplayOrder(String symbol, long seqNo, Long userId, UUID clientOrderId,
                            OrderSide side, BigDecimal limitPrice, long originalQuantity) {
            this.symbol = symbol;
            this.seqNo = seqNo;
            this.userId = userId;
            this.clientOrderId = clientOrderId;
            this.side = side;
            this.limitPrice = limitPrice;
            this.originalQuantity = originalQuantity;
            this.remainingQuantity = originalQuantity;
        }

        private boolean isOpen() {
            return (status == OrderStatus.NEW || status == OrderStatus.PARTIALLY_FILLED)
                    && remainingQuantity > 0;
        }

        private void fill(long quantity) {
            remainingQuantity -= quantity;
            status = remainingQuantity == 0
                    ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED;
        }

        private RecoveredOrder toRecoveredOrder() {
            return new RecoveredOrder(symbol, seqNo, userId, clientOrderId, side,
                    limitPrice, originalQuantity, remainingQuantity, status);
        }
    }
}
