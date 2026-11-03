package com.tradeguard.order;

import com.tradeguard.common.PageableSortValidator;
import com.tradeguard.instrument.dto.PageResponse;
import com.tradeguard.order.dto.CreateOrderRequest;
import com.tradeguard.order.dto.OrderResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Set;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(
            Authentication authentication, @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse created = orderService.create(authentication.getName(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.orderId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/mine")
    public ResponseEntity<PageResponse<OrderResponse>> findMine(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "id",
                    direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {
        PageableSortValidator.requireAllowed(pageable, Set.of("id"));
        return ResponseEntity.ok(PageResponse.from(
                orderService.findMine(authentication.getName(), pageable)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<OrderResponse> cancel(Authentication authentication,
                                                 @PathVariable long id) {
        return ResponseEntity.ok(orderService.cancel(authentication.getName(), id));
    }
}
