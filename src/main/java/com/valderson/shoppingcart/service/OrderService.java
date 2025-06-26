package com.valderson.shoppingcart.service;

import com.valderson.shoppingcart.dto.response.OrderItemResponse;
import com.valderson.shoppingcart.dto.response.OrderResponse;
import com.valderson.shoppingcart.entity.CartItem;
import com.valderson.shoppingcart.entity.Order;
import com.valderson.shoppingcart.entity.OrderItem;
import com.valderson.shoppingcart.entity.Product;
import com.valderson.shoppingcart.entity.ShoppingCart;
import com.valderson.shoppingcart.enums.OrderStatus;
import com.valderson.shoppingcart.repository.CartItemRepository;
import com.valderson.shoppingcart.repository.OrderItemRepository;
import com.valderson.shoppingcart.repository.OrderRepository;
import com.valderson.shoppingcart.repository.ProductRepository;
import com.valderson.shoppingcart.repository.ShoppingCartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShoppingCartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;

    public OrderResponse createOrder(Long userId) {
        // Buscar carrinho do usuário
        ShoppingCart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Carrinho não encontrado"));

        List<CartItem> cartItems = cartItemRepository.findByShoppingCartId(cart.getId());

        if (cartItems.isEmpty()) {
            throw new RuntimeException("Carrinho está vazio");
        }

        // Calcular total do pedido
        BigDecimal totalAmount = calculateOrderTotal(cartItems);

        // Criar pedido
        Order order = Order.builder()
                .userId(userId)
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .build();

        Order savedOrder = orderRepository.save(order);

        // Copiar itens do carrinho para o pedido (snapshot)
        List<OrderItem> orderItems = cartItems.stream()
                .map(cartItem -> createOrderItemFromCartItem(savedOrder.getId(), cartItem))
                .collect(Collectors.toList());

        orderItemRepository.saveAll(orderItems);

        // Limpar carrinho após criação do pedido
        cartService.clearCart(userId);

        return mapToOrderResponse(savedOrder, orderItems);
    }

    public List<OrderResponse> getUserOrders(Long userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return orders.stream()
                .map(order -> {
                    List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
                    return mapToOrderResponse(order, items);
                })
                .collect(Collectors.toList());
    }

    public OrderResponse cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Pedido não pertence ao usuário");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Apenas pedidos pendentes podem ser cancelados");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        return mapToOrderResponse(savedOrder, items);
    }

    private BigDecimal calculateOrderTotal(List<CartItem> cartItems) {
        return cartItems.stream()
                .map(item -> {
                    Product product = productRepository.findById(item.getProductId())
                            .orElseThrow(() -> new RuntimeException("Produto não encontrado"));
                    return product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private OrderItem createOrderItemFromCartItem(Long orderId, CartItem cartItem) {
        Product product = productRepository.findById(cartItem.getProductId())
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

        BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));

        return OrderItem.builder()
                .orderId(orderId)
                .productId(product.getId())
                .productName(product.getName())
                .productPrice(product.getPrice())
                .quantity(cartItem.getQuantity())
                .subtotal(subtotal)
                .build();
    }

    private OrderResponse mapToOrderResponse(Order order, List<OrderItem> items) {
        List<OrderItemResponse> itemResponses = items.stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .productPrice(item.getProductPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .items(itemResponses)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }
}