package com.valderson.shoppingcart.service;

import com.valderson.shoppingcart.dto.response.OrderItemResponse;
import com.valderson.shoppingcart.dto.response.OrderResponse;
import com.valderson.shoppingcart.entity.CartItem;
import com.valderson.shoppingcart.entity.Order;
import com.valderson.shoppingcart.entity.OrderItem;
import com.valderson.shoppingcart.entity.Product;
import com.valderson.shoppingcart.entity.ShoppingCart;
import com.valderson.shoppingcart.entity.User;
import com.valderson.shoppingcart.enums.OrderStatus;
import com.valderson.shoppingcart.repository.CartItemRepository;
import com.valderson.shoppingcart.repository.OrderItemRepository;
import com.valderson.shoppingcart.repository.OrderRepository;
import com.valderson.shoppingcart.repository.ProductRepository;
import com.valderson.shoppingcart.repository.ShoppingCartRepository;
import com.valderson.shoppingcart.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final CartService cartService;

    public OrderResponse createOrder(Long userId) {
        // Buscar usuário
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Buscar carrinho do usuário
        ShoppingCart cart = user.getShoppingCart();
        if (cart == null) {
            throw new RuntimeException("Carrinho não encontrado");
        }

        List<CartItem> cartItems = cart.getCartItems();

        if (cartItems == null || cartItems.isEmpty()) {
            throw new RuntimeException("Carrinho está vazio");
        }

        // Calcular total do pedido
        BigDecimal totalAmount = calculateOrderTotal(cartItems);

        // Criar pedido
        Order order = Order.builder()
                .user(user)
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .build();

        Order savedOrder = orderRepository.save(order);

        // Copiar itens do carrinho para o pedido (snapshot)
        List<OrderItem> orderItems = cartItems.stream()
                .map(cartItem -> createOrderItemFromCartItem(savedOrder, cartItem))
                .collect(Collectors.toList());

        orderItemRepository.saveAll(orderItems);

        // Limpar carrinho após criação do pedido
        cartService.clearCart(userId);

        return mapToOrderResponse(savedOrder, orderItems);
    }

    public List<OrderResponse> getUserOrders(Long userId) {
        // Buscar usuário
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        List<Order> orders = user.getOrders();

        return orders.stream()
                .map(order -> {
                    List<OrderItem> items = order.getOrderItems();
                    return mapToOrderResponse(order, items);
                })
                .collect(Collectors.toList());
    }

    public OrderResponse cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Pedido não pertence ao usuário");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Apenas pedidos pendentes podem ser cancelados");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);

        List<OrderItem> items = savedOrder.getOrderItems();
        return mapToOrderResponse(savedOrder, items);
    }

    private BigDecimal calculateOrderTotal(List<CartItem> cartItems) {
        return cartItems.stream()
                .map(item -> {
                    Product product = item.getProduct();
                    return product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private OrderItem createOrderItemFromCartItem(Order order, CartItem cartItem) {
        Product product = cartItem.getProduct();

        BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));

        return OrderItem.builder()
                .order(order)
                .product(product)
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
                        .productId(item.getProduct().getId())
                        .productName(item.getProductName())
                        .productPrice(item.getProductPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .items(itemResponses)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }
}