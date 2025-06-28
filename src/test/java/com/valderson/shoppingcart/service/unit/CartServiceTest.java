package com.valderson.shoppingcart.service.unit;

import com.valderson.shoppingcart.dto.request.AddToCartRequest;
import com.valderson.shoppingcart.dto.response.CartResponse;
import com.valderson.shoppingcart.entity.*;
import com.valderson.shoppingcart.repository.*;
import com.valderson.shoppingcart.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CartService - Testes Unitários")
class CartServiceTest {

    @Mock private ShoppingCartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    private User mockUser;
    private ShoppingCart mockCart;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(1L).build();
        mockCart = ShoppingCart.builder()
                .id(100L)
                .user(mockUser)
                .cartItems(new ArrayList<>())
                .build();
        mockUser.setShoppingCart(mockCart);

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(cartRepository.save(any())).thenReturn(mockCart);

        // Importante: simula o save adicionando item ao carrinho
        doAnswer(invocation -> {
            CartItem item = invocation.getArgument(0);
            mockCart.getCartItems().add(item);
            return item;
        }).when(cartItemRepository).save(any());

        // Simula retorno dos itens com produtos ao calcular total
        when(cartItemRepository.findByShoppingCartIdWithProduct(any()))
                .thenAnswer(invocation -> new ArrayList<>(mockCart.getCartItems()));
    }

    @ParameterizedTest
    @CsvSource({
            "10, 1", "10, 3", "10, 5"
    })
    @DisplayName("Deve adicionar item ao carrinho com diferentes quantidades")
    void shouldAddItemsWithDifferentQuantities(Long productId, Integer quantity) {
        Product mockProduct = Product.builder().id(productId).price(BigDecimal.TEN).build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));

        AddToCartRequest request = AddToCartRequest.builder()
                .productId(productId)
                .quantity(quantity)
                .build();

        CartResponse response = cartService.addItemToCart(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(quantity);
    }

    @ParameterizedTest
    @CsvSource({
            "10.00, 2, 20.00",
            "5.00, 1, 5.00",
            "2.50, 4, 10.00"
    })
    @DisplayName("Deve calcular o total do carrinho corretamente")
    void shouldCalculateCartTotal(String priceStr, int quantity, String expectedTotalStr) {
        BigDecimal price = new BigDecimal(priceStr);
        BigDecimal expectedTotal = new BigDecimal(expectedTotalStr);

        Product product = Product.builder().id(100L).price(price).build();
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));

        AddToCartRequest request = AddToCartRequest.builder()
                .productId(100L)
                .quantity(quantity)
                .build();

        cartService.addItemToCart(1L, request);

        BigDecimal total = cartService.getCartTotal(1L);
        assertThat(total).isEqualByComparingTo(expectedTotal);
    }

    @ParameterizedTest
    @CsvSource({ "10", "20" })
    @DisplayName("Deve remover item do carrinho")
    void shouldRemoveItemFromCart(Long productId) {
        Product product = Product.builder().id(productId).price(BigDecimal.TEN).build();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        AddToCartRequest request = AddToCartRequest.builder()
                .productId(productId)
                .quantity(1)
                .build();

        cartService.addItemToCart(1L, request);

        CartResponse response = cartService.removeItemFromCart(1L, productId);

        assertThat(response.getItems()).noneMatch(i -> i.getProductId().equals(productId));
    }

    @ParameterizedTest
    @CsvSource({ "999", "888", "777" })
    @DisplayName("Deve lançar exceção se produto não for encontrado")
    void shouldThrowExceptionWhenProductNotFound(Long invalidProductId) {
        AddToCartRequest request = AddToCartRequest.builder()
                .productId(invalidProductId)
                .quantity(1)
                .build();

        when(productRepository.findById(invalidProductId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItemToCart(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Produto não encontrado");
    }

    @ParameterizedTest
    @CsvSource({ "999", "888", "777" })
    @DisplayName("Deve lançar exceção se usuário não for encontrado")
    void shouldThrowExceptionWhenUserNotFound(Long invalidUserId) {
        when(userRepository.findById(invalidUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.getCartByUserId(invalidUserId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Usuário não encontrado");
    }
}