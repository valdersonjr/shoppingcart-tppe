package com.valderson.shoppingcart.repository;

import com.valderson.shoppingcart.entity.Order;
import com.valderson.shoppingcart.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);
}