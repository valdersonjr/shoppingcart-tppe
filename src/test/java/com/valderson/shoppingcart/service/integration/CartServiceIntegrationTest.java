package com.valderson.shoppingcart.service.integration;

import com.valderson.shoppingcart.dto.request.AddToCartRequest;
import com.valderson.shoppingcart.entity.*;
import com.valderson.shoppingcart.repository.*;
import com.valderson.shoppingcart.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("CartService - Teste de Integração")
class CartServiceIntegrationTest {

    @Autowired private CartService cartService;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ShoppingCartRepository cartRepository;
    @Autowired private CartItemRepository cartItemRepository;

    private User user;

    @BeforeEach
    void setUp() {
        // Criar e salvar usuário com carrinho
        user = userRepository.save(User.builder()
                .name("Usuário Teste")
                .email("teste@exemplo.com")
                .passwordHash("senha123")
                .build());

        ShoppingCart cart = cartRepository.save(ShoppingCart.builder()
                .user(user)
                .cartItems(List.of())
                .build());

        user.setShoppingCart(cart);
        userRepository.save(user);
    }

    @ParameterizedTest
    @CsvSource({
            "Pepsi, 4.50, 3",
            "Coca-Cola, 5.00, 2",
            "Guaraná, 3.25, 5"
    })
    @DisplayName("Deve adicionar item ao carrinho com sucesso para diferentes produtos e quantidades")
    void shouldAddItemToCartSuccessfully(String productName, BigDecimal price, int quantity) {
        // Dado um produto salvo no banco
        Product product = productRepository.save(Product.builder()
                .name(productName)
                .price(price)
                .build());

        AddToCartRequest request = AddToCartRequest.builder()
                .productId(product.getId())
                .quantity(quantity)
                .build();

        // Quando adiciona item ao carrinho
        cartService.addItemToCart(user.getId(), request);

        // Então o item está no carrinho
        List<CartItem> items = cartItemRepository.findAllByShoppingCartId(user.getShoppingCart().getId());
        assertThat(items).hasSize(1);

        CartItem item = items.get(0);
        assertThat(item.getQuantity()).isEqualTo(quantity);
        assertThat(item.getProduct().getId()).isEqualTo(product.getId());

        BigDecimal expectedTotal = price.multiply(BigDecimal.valueOf(quantity));
        BigDecimal actualTotal = cartService.getCartTotal(user.getId());
        assertThat(actualTotal).isEqualByComparingTo(expectedTotal);
    }

    @Test
    @DisplayName("Deve limpar carrinho com sucesso")
    void shouldClearCartSuccessfully() {
        // Dado produto e item no carrinho
        Product product = productRepository.save(Product.builder()
                .name("Sprite")
                .price(new BigDecimal("3.99"))
                .build());

        cartItemRepository.save(CartItem.builder()
                .shoppingCart(user.getShoppingCart())
                .product(product)
                .quantity(2)
                .build());

        assertThat(cartItemRepository.findAllByShoppingCartId(user.getShoppingCart().getId()))
                .hasSize(1);

        // Quando limpa o carrinho
        cartService.clearCart(user.getId());

        // Então o carrinho está vazio
        assertThat(cartItemRepository.findAllByShoppingCartId(user.getShoppingCart().getId()))
                .isEmpty();

        BigDecimal total = cartService.getCartTotal(user.getId());
        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }
}