package com.valderson.shoppingcart.service.unit;

import com.valderson.shoppingcart.dto.response.OrderResponse;
import com.valderson.shoppingcart.entity.*;
import com.valderson.shoppingcart.enums.OrderStatus;
import com.valderson.shoppingcart.repository.*;
import com.valderson.shoppingcart.service.CartService;
import com.valderson.shoppingcart.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OrderService - Testes Unitários")
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private ShoppingCartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private CartService cartService;

    @InjectMocks
    private OrderService orderService;

    private User user;
    private ShoppingCart cart;
    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .name("Produto Teste")
                .price(BigDecimal.TEN)
                .build();

        cart = ShoppingCart.builder()
                .id(1L)
                .cartItems(new ArrayList<>())
                .build();

        user = User.builder()
                .id(1L)
                .shoppingCart(cart)
                .orders(new ArrayList<>())
                .build();
        cart.setUser(user);
    }

    @ParameterizedTest(name = "Qtd: {0}, Preço: {1}, Total: {2}")
    @CsvSource({
            "2, 10.00, 20.00",
            "3, 5.00, 15.00",
            "1, 99.99, 99.99"
    })
    @DisplayName("Deve criar pedido com sucesso a partir do carrinho (parametrizado)")
    void shouldCreateOrderSuccessfullyParameterized(int quantity, String unitPriceStr, String expectedTotalStr) {
        // Given
        BigDecimal unitPrice = new BigDecimal(unitPriceStr);
        BigDecimal expectedTotal = new BigDecimal(expectedTotalStr);

        Product p = Product.builder()
                .id(1L)
                .name("Produto Teste")
                .price(unitPrice)
                .build();

        CartItem item = CartItem.builder()
                .id(1L)
                .product(p)
                .quantity(quantity)
                .build();

        cart.setCartItems(List.of(item));
        user.setShoppingCart(cart);

        Order savedOrder = Order.builder()
                .id(100L)
                .user(user)
                .status(OrderStatus.PENDING)
                .totalAmount(expectedTotal)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderItemRepository.saveAll(anyList())).thenReturn(null);

        // When
        OrderResponse response = orderService.createOrder(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getTotalAmount()).isEqualByComparingTo(expectedTotal);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(quantity);

        verify(cartService).clearCart(1L);
    }

    // ---- testes não modificados abaixo ----

    @Test
    @DisplayName("Deve lançar exceção se usuário não existir ao criar pedido")
    void shouldThrowIfUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Usuário não encontrado");
    }

    @Test
    @DisplayName("Deve lançar exceção se carrinho estiver vazio")
    void shouldThrowIfCartIsEmpty() {
        cart.setCartItems(new ArrayList<>());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> orderService.createOrder(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Carrinho está vazio");
    }

    @Test
    @DisplayName("Deve buscar pedidos do usuário")
    void shouldGetUserOrders() {
        Order order = Order.builder()
                .id(10L)
                .user(user)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.TEN)
                .orderItems(List.of(
                        OrderItem.builder()
                                .id(1L)
                                .order(null)
                                .product(product)
                                .productName(product.getName())
                                .productPrice(product.getPrice())
                                .quantity(1)
                                .subtotal(product.getPrice())
                                .build()
                ))
                .createdAt(LocalDateTime.now())
                .build();

        user.setOrders(List.of(order));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        List<OrderResponse> responses = orderService.getUserOrders(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getItems()).hasSize(1);
        assertThat(responses.get(0).getTotalAmount()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("Deve cancelar pedido com sucesso")
    void shouldCancelOrderSuccessfully() {
        Order order = Order.builder()
                .id(123L)
                .user(user)
                .status(OrderStatus.PENDING)
                .orderItems(List.of(
                        OrderItem.builder()
                                .id(1L)
                                .product(product)
                                .productName("Produto Teste")
                                .productPrice(BigDecimal.TEN)
                                .quantity(1)
                                .subtotal(BigDecimal.TEN)
                                .build()
                ))
                .totalAmount(BigDecimal.TEN)
                .build();

        when(orderRepository.findById(123L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.cancelOrder(1L, 123L);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("Deve lançar exceção ao cancelar pedido de outro usuário")
    void shouldThrowWhenCancelOrderFromAnotherUser() {
        Order order = Order.builder()
                .id(50L)
                .user(User.builder().id(999L).build())
                .status(OrderStatus.PENDING)
                .build();

        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, 50L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Pedido não pertence ao usuário");
    }

    @Test
    @DisplayName("Deve lançar exceção ao cancelar pedido não pendente")
    void shouldThrowWhenCancelNonPendingOrder() {
        Order order = Order.builder()
                .id(77L)
                .user(user)
                .status(OrderStatus.CONFIRMED)
                .build();

        when(orderRepository.findById(77L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, 77L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Apenas pedidos pendentes podem ser cancelados");
    }
}