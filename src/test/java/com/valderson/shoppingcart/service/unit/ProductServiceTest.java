package com.valderson.shoppingcart.service.unit;

import com.valderson.shoppingcart.dto.response.ProductResponse;
import com.valderson.shoppingcart.entity.Product;
import com.valderson.shoppingcart.repository.ProductRepository;
import com.valderson.shoppingcart.service.ProductService;
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
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ProductService - Testes Unitários")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product mockProduct;

    @BeforeEach
    void setUp() {
        mockProduct = Product.builder()
                .id(1L)
                .name("Produto Teste")
                .description("Descrição do produto")
                .price(new BigDecimal("19.99"))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Deve retornar todos os produtos em ordem decrescente de criação")
    void shouldReturnAllProductsOrderedByCreatedAtDesc() {
        // Given
        List<Product> products = List.of(mockProduct);
        when(productRepository.findAllByOrderByCreatedAtDesc()).thenReturn(products);

        // When
        List<ProductResponse> responses = productService.getAllProducts();

        // Then
        assertThat(responses).hasSize(1);
        ProductResponse response = responses.get(0);
        assertThat(response.getId()).isEqualTo(mockProduct.getId());
        assertThat(response.getName()).isEqualTo(mockProduct.getName());
        assertThat(response.getDescription()).isEqualTo(mockProduct.getDescription());
        assertThat(response.getPrice()).isEqualByComparingTo(mockProduct.getPrice());
    }

    @ParameterizedTest(name = "Produto ID: {0}, Nome: {1}, Descrição: {2}, Preço: {3}")
    @CsvSource({
            "1, Produto A, Descrição A, 10.00",
            "2, Produto B, Descrição B, 20.50",
            "3, Produto C, Descrição C, 30.99"
    })
    @DisplayName("Deve retornar produto por ID com sucesso (parametrizado)")
    void shouldReturnProductByIdParameterized(Long id, String name, String description, String priceStr) {
        // Given
        BigDecimal price = new BigDecimal(priceStr);
        Product product = Product.builder()
                .id(id)
                .name(name)
                .description(description)
                .price(price)
                .createdAt(LocalDateTime.now())
                .build();

        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        // When
        ProductResponse response = productService.getProductById(id);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getName()).isEqualTo(name);
        assertThat(response.getDescription()).isEqualTo(description);
        assertThat(response.getPrice()).isEqualByComparingTo(price);
    }

    @Test
    @DisplayName("Deve lançar exceção se produto não for encontrado por ID")
    void shouldThrowExceptionWhenProductNotFound() {
        // Given
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        // Then
        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Produto não encontrado");
    }
}