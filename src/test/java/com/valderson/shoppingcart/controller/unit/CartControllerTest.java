package com.valderson.shoppingcart.controller.unit;

import com.valderson.shoppingcart.controller.CartController;
import com.valderson.shoppingcart.dto.request.AddToCartRequest;
import com.valderson.shoppingcart.dto.response.CartItemResponse;
import com.valderson.shoppingcart.dto.response.CartResponse;
import com.valderson.shoppingcart.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.OK;

public class CartControllerTest {

    private CartService cartService;
    private CartController cartController;

    @BeforeEach
    void setUp() {
        cartService = mock(CartService.class);
        cartController = new CartController(cartService);
    }

    private CartResponse createCartResponse(Long id, Long userId, BigDecimal total) {
        List<CartItemResponse> items = List.of(
                new CartItemResponse(1L, 10L, "Product 1", new BigDecimal("10.00"), 2, new BigDecimal("20.00")),
                new CartItemResponse(2L, 20L, "Product 2", new BigDecimal("5.00"), 1, new BigDecimal("5.00"))
        );

        return CartResponse.builder()
                .id(id)
                .userId(userId)
                .items(items)
                .totalAmount(total)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Deve retornar o carrinho do usuário com sucesso")
    void testGetCart() {
        Long userId = 1L;
        CartResponse mockCart = createCartResponse(100L, userId, new BigDecimal("25.00"));

        when(cartService.getCartByUserId(userId)).thenReturn(mockCart);

        ResponseEntity<CartResponse> response = cartController.getCart(userId);

        verify(cartService).getCartByUserId(userId);
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody())
                .usingRecursiveComparison()
                .isEqualTo(mockCart);
    }

    @Test
    @DisplayName("Deve adicionar item ao carrinho e retornar carrinho atualizado")
    void testAddItemToCart() {
        Long userId = 2L;
        AddToCartRequest request = new AddToCartRequest(10L, 3);
        CartResponse updatedCart = createCartResponse(101L, userId, new BigDecimal("25.00"));

        when(cartService.addItemToCart(userId, request)).thenReturn(updatedCart);

        ResponseEntity<CartResponse> response = cartController.addItemToCart(userId, request);

        verify(cartService).addItemToCart(userId, request);
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody())
                .usingRecursiveComparison()
                .isEqualTo(updatedCart);
    }

    @Test
    @DisplayName("Deve remover item do carrinho e retornar carrinho atualizado")
    void testRemoveItemFromCart() {
        Long userId = 3L;
        Long productId = 99L;
        CartResponse updatedCart = createCartResponse(102L, userId, new BigDecimal("25.00"));

        when(cartService.removeItemFromCart(userId, productId)).thenReturn(updatedCart);

        ResponseEntity<CartResponse> response = cartController.removeItemFromCart(userId, productId);

        verify(cartService).removeItemFromCart(userId, productId);
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody())
                .usingRecursiveComparison()
                .isEqualTo(updatedCart);
    }

    @Test
    @DisplayName("Deve retornar o valor total do carrinho")
    void testGetCartTotal() {
        Long userId = 4L;
        BigDecimal total = new BigDecimal("123.45");

        when(cartService.getCartTotal(userId)).thenReturn(total);

        ResponseEntity<BigDecimal> response = cartController.getCartTotal(userId);

        verify(cartService).getCartTotal(userId);
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isEqualTo(total);
    }

    @Test
    @DisplayName("Deve limpar o carrinho e retornar mensagem de sucesso")
    void testClearCart() {
        Long userId = 5L;

        ResponseEntity<String> response = cartController.clearCart(userId);

        verify(cartService).clearCart(userId);
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isEqualTo("Carrinho limpo com sucesso");
    }
}