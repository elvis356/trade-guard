package com.tradeguard.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders", uniqueConstraints = {
        @UniqueConstraint(name = "uk_orders_user_client_id", columnNames = {"user_id", "client_order_id"}),
        @UniqueConstraint(name = "uk_orders_symbol_seq_no", columnNames = {"symbol", "seq_no"})
})
public class TradingOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_order_id", nullable = false)
    private UUID clientOrderId;

    @Column(name = "seq_no", nullable = false)
    private long seqNo;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 16)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private OrderSide side;

    @Column(name = "limit_price", nullable = false, precision = 12, scale = 4)
    private BigDecimal limitPrice;

    @Column(name = "original_quantity", nullable = false)
    private long originalQuantity;

    @Column(name = "remaining_quantity", nullable = false)
    private long remainingQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TradingOrder() {
    }

    public TradingOrder(UUID clientOrderId, long seqNo, Long userId, String symbol, OrderSide side,
                       BigDecimal limitPrice, long quantity) {
        this.clientOrderId = clientOrderId;
        this.seqNo = seqNo;
        this.userId = userId;
        this.symbol = symbol;
        this.side = side;
        this.limitPrice = limitPrice;
        this.originalQuantity = quantity;
        this.remainingQuantity = quantity;
        this.status = OrderStatus.NEW;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void fill(long quantity) {
        if (quantity <= 0 || quantity > remainingQuantity) {
            throw new IllegalArgumentException("Fill quantity must be within remaining quantity.");
        }
        remainingQuantity -= quantity;
        status = remainingQuantity == 0 ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED;
    }

    public void cancel() {
        if (remainingQuantity == 0 || status == OrderStatus.CANCELED) {
            throw new IllegalStateException("Order has no cancellable quantity.");
        }
        status = OrderStatus.CANCELED;
    }

    public Long getId() {
        return id;
    }

    public UUID getClientOrderId() {
        return clientOrderId;
    }

    public long getSeqNo() {
        return seqNo;
    }

    public Long getUserId() {
        return userId;
    }

    public String getSymbol() {
        return symbol;
    }

    public OrderSide getSide() {
        return side;
    }

    public BigDecimal getLimitPrice() {
        return limitPrice;
    }

    public long getOriginalQuantity() {
        return originalQuantity;
    }

    public long getRemainingQuantity() {
        return remainingQuantity;
    }

    public long getFilledQuantity() {
        return originalQuantity - remainingQuantity;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
