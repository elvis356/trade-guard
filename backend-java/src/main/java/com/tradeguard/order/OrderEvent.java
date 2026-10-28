package com.tradeguard.order;

import com.tradeguard.order.dto.CreateOrderRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_events")
public class OrderEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 24)
    private OrderEventType eventType;

    @Column(nullable = false, length = 16)
    private String symbol;

    @Column(name = "order_seq_no", nullable = false)
    private long orderSeqNo;

    @Column(name = "counterparty_seq_no")
    private Long counterpartySeqNo;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "client_order_id")
    private UUID clientOrderId;

    @Enumerated(EnumType.STRING)
    @Column(length = 4)
    private OrderSide side;

    @Column(name = "limit_price", precision = 12, scale = 4)
    private BigDecimal limitPrice;

    private Long quantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OrderEvent() {
    }

    private OrderEvent(OrderEventType eventType, String symbol, long orderSeqNo,
                       Long counterpartySeqNo, Long userId, UUID clientOrderId,
                       OrderSide side, BigDecimal limitPrice, Long quantity) {
        this.eventType = eventType;
        this.symbol = symbol;
        this.orderSeqNo = orderSeqNo;
        this.counterpartySeqNo = counterpartySeqNo;
        this.userId = userId;
        this.clientOrderId = clientOrderId;
        this.side = side;
        this.limitPrice = limitPrice;
        this.quantity = quantity;
    }

    public static OrderEvent accepted(CreateOrderRequest request, Long userId, long seqNo) {
        return new OrderEvent(OrderEventType.ORDER_ACCEPTED, request.symbol(), seqNo, null,
                userId, request.clientOrderId(), request.side(), request.price(),
                request.qty());
    }

    public static OrderEvent canceled(String symbol, long seqNo) {
        return new OrderEvent(OrderEventType.ORDER_CANCELED, symbol, seqNo, null,
                null, null, null, null, null);
    }

    public static OrderEvent tradeExecuted(String symbol, long orderSeqNo,
                                           long counterpartySeqNo, long quantity) {
        return new OrderEvent(OrderEventType.TRADE_EXECUTED, symbol, orderSeqNo,
                counterpartySeqNo, null, null, null, null, quantity);
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public OrderEventType getEventType() {
        return eventType;
    }

    public String getSymbol() {
        return symbol;
    }

    public long getOrderSeqNo() {
        return orderSeqNo;
    }

    public Long getCounterpartySeqNo() {
        return counterpartySeqNo;
    }

    public Long getUserId() {
        return userId;
    }

    public UUID getClientOrderId() {
        return clientOrderId;
    }

    public OrderSide getSide() {
        return side;
    }

    public BigDecimal getLimitPrice() {
        return limitPrice;
    }

    public Long getQuantity() {
        return quantity;
    }
}
