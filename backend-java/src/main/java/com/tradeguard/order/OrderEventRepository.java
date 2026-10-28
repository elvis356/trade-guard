package com.tradeguard.order;

import org.springframework.data.repository.Repository;

import java.util.List;

public interface OrderEventRepository extends Repository<OrderEvent, Long> {

    OrderEvent saveAndFlush(OrderEvent event);

    List<OrderEvent> findAllByOrderByIdAsc();
}
