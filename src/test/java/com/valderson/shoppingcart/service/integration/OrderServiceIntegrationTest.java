package com.valderson.shoppingcart.service.integration;

import com.valderson.shoppingcart.dto.request.AddToCartRequest;
import com.valderson.shoppingcart.dto.response.OrderResponse;
import com.valderson.shoppingcart.entity.Product;
import com.valderson.shoppingcart.entity.ShoppingCart;
import com.valderson.shoppingcart.entity.User;
import com.valderson.shoppingcart.enums.OrderStatus;
import com.valderson.shoppingcart.repository.*;
import com.valderson.shoppingcart.service.CartService;
import com.valderson.shoppingcart.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("OrderService - Testes de Integração")
class OrderServiceIntegrationTest {

    @Autowired private OrderService orderService;
    @Autowired private CartService cartService;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        // Limpar dados
        orderRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();

        // Criar usuário
        user = User.builder()
                .name("Usuário Teste")
                .email("teste@email.com")
                .passwordHash(passwordEncoder.encode("senha123"))
                .build();

        // Criar carrinho e associar usuário
        ShoppingCart cart = new ShoppingCart();
        cart.setUser(user);
        user.setShoppingCart(cart);

        user = userRepository.save(user);

        // Criar produto
        product = productRepository.save(Product.builder()
                .name("Produto Teste")
                .price(BigDecimal.valueOf(10.00))
                .build());

        // Adicionar item ao carrinho
        AddToCartRequest request = AddToCartRequest.builder()
                .productId(product.getId())
                .quantity(3)
                .build();

        cartService.addItemToCart(user.getId(), request); // total: 30.00
    }

    @Test
    @DisplayName("Deve criar pedido com sucesso a partir do carrinho")
    void shouldCreateOrderSuccessfully() {
        OrderResponse order = orderService.createOrder(user.getId());

        assertThat(order).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("30.00");
    }

    @Test
    @DisplayName("Deve listar pedidos do usuário")
    void shouldListUserOrders() {
        orderService.createOrder(user.getId());
        List<OrderResponse> orders = orderService.getUserOrders(user.getId());

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getItems()).hasSize(1);
        assertThat(orders.get(0).getTotalAmount()).isEqualByComparingTo("30.00");
    }

    @Test
    @DisplayName("Deve cancelar pedido pendente com sucesso")
    void shouldCancelPendingOrder() {
        OrderResponse order = orderService.createOrder(user.getId());
        OrderResponse cancelled = orderService.cancelOrder(user.getId(), order.getId());

        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("Deve lançar exceção ao cancelar pedido de outro usuário")
    void shouldFailToCancelOrderFromAnotherUser() {
        final OrderResponse order = orderService.createOrder(user.getId());

        // Criar outro usuário com carrinho vinculado corretamente
        final User otherUser = User.builder()
                .name("Outro")
                .email("outro@email.com")
                .passwordHash(passwordEncoder.encode("outraSenha"))
                .build();

        ShoppingCart otherCart = new ShoppingCart();
        otherCart.setUser(otherUser);
        otherUser.setShoppingCart(otherCart);

        userRepository.save(otherUser);

        assertThatThrownBy(() -> orderService.cancelOrder(otherUser.getId(), order.getId()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Pedido não pertence ao usuário");
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar pedido com carrinho vazio")
    void shouldFailWhenCartIsEmpty() {
        cartService.clearCart(user.getId());

        assertThatThrownBy(() -> orderService.createOrder(user.getId()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Carrinho está vazio");
    }
}