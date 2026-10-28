package com.tradeguard.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<TradingOrder, Long> {

    Optional<TradingOrder> findByUserIdAndClientOrderId(Long userId, UUID clientOrderId);

    Optional<TradingOrder> findByIdAndUserId(Long id, Long userId);

    Page<TradingOrder> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    List<TradingOrder> findBySymbolAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByLimitPriceAscSeqNoAsc(
            String symbol, OrderSide side, Collection<OrderStatus> statuses, long remainingQuantity);

    List<TradingOrder> findBySymbolAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByLimitPriceDescSeqNoAsc(
            String symbol, OrderSide side, Collection<OrderStatus> statuses, long remainingQuantity);

    @org.springframework.data.jpa.repository.Query(
            "select max(o.seqNo) from TradingOrder o where o.symbol = :symbol")
    Long findMaxSeqNoBySymbol(@org.springframework.data.repository.query.Param("symbol") String symbol);
}
