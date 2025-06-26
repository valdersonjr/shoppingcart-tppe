package com.valderson.shoppingcart.controller;

import com.valderson.shoppingcart.dto.response.OrderResponse;
import com.valderson.shoppingcart.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/{userId}")
    public ResponseEntity<OrderResponse> createOrder(@PathVariable Long userId) {
        OrderResponse order = orderService.createOrder(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<OrderResponse>> getUserOrders(@PathVariable Long userId) {
        List<OrderResponse> orders = orderService.getUserOrders(userId);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/{userId}/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long userId,
                                                     @PathVariable Long orderId) {
        OrderResponse order = orderService.cancelOrder(userId, orderId);
        return ResponseEntity.ok(order);
    }
}