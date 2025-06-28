package com.valderson.shoppingcart.service.integration;

import com.valderson.shoppingcart.dto.response.ProductResponse;
import com.valderson.shoppingcart.entity.Product;
import com.valderson.shoppingcart.repository.ProductRepository;
import com.valderson.shoppingcart.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ProductService - Testes de Integração")
class ProductServiceIntegrationTest {

    @Autowired private ProductService productService;
    @Autowired private ProductRepository productRepository;

    private Product savedProduct1;
    private Product savedProduct2;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();

        savedProduct1 = productRepository.save(Product.builder()
                .name("Produto A")
                .description("Descrição A")
                .price(BigDecimal.valueOf(15.50))
                .build());

        savedProduct2 = productRepository.save(Product.builder()
                .name("Produto B")
                .description("Descrição B")
                .price(BigDecimal.valueOf(22.00))
                .build());
    }

    @Test
    @DisplayName("Deve retornar todos os produtos em ordem decrescente de criação")
    void shouldReturnAllProducts() {
        List<ProductResponse> products = productService.getAllProducts();

        assertThat(products).hasSize(2);
        assertThat(products.get(0).getId()).isEqualTo(savedProduct2.getId());
        assertThat(products.get(1).getId()).isEqualTo(savedProduct1.getId());
    }

    @ParameterizedTest(name = "Deve retornar corretamente o produto: {1}")
    @CsvSource({
            "1,Produto A,Descrição A,15.50",
            "2,Produto B,Descrição B,22.00"
    })
    void shouldReturnProductByIdParameterized(int index, String expectedName, String expectedDescription, String expectedPrice) {
        Long id = (index == 1) ? savedProduct1.getId() : savedProduct2.getId();

        ProductResponse response = productService.getProductById(id);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getName()).isEqualTo(expectedName);
        assertThat(response.getDescription()).isEqualTo(expectedDescription);
        assertThat(response.getPrice()).isEqualByComparingTo(expectedPrice);
    }

    @ParameterizedTest
    @CsvSource({"999", "1234", "9999999"})
    @DisplayName("Deve lançar exceção ao buscar produto inexistente")
    void shouldThrowWhenProductNotFound(Long invalidId) {
        assertThatThrownBy(() -> productService.getProductById(invalidId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Produto não encontrado");
    }
}