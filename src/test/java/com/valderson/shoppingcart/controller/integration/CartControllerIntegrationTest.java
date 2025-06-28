package com.valderson.shoppingcart.controller.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valderson.shoppingcart.dto.request.AddToCartRequest;
import com.valderson.shoppingcart.entity.Product;
import com.valderson.shoppingcart.entity.User;
import com.valderson.shoppingcart.repository.CartItemRepository;
import com.valderson.shoppingcart.repository.ProductRepository;
import com.valderson.shoppingcart.repository.ShoppingCartRepository;
import com.valderson.shoppingcart.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CartControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ShoppingCartRepository shoppingCartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    private User testUser;
    private Product testProduct1;

    @BeforeEach
    void setUp() {
        // Configurar MockMvc
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();

        // Limpar dados de teste
        cartItemRepository.deleteAll();
        shoppingCartRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        // Criar usuário de teste
        testUser = User.builder()
                .name("Test User")
                .email("test@example.com")
                .passwordHash("hashedpassword")
                .build();
        testUser = userRepository.save(testUser);

        // Criar produto de teste
        testProduct1 = Product.builder()
                .name("Produto 1")
                .description("Descrição do produto 1")
                .price(new BigDecimal("10.50"))
                .build();
        testProduct1 = productRepository.save(testProduct1);
    }

    @Test
    @DisplayName("Deve retornar carrinho vazio quando usuário não tem itens")
    void getCart_WhenEmpty_ShouldReturnEmptyCart() throws Exception {
        mockMvc.perform(get("/api/cart/{userId}", testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(testUser.getId().intValue())))
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalAmount", is(0)));
    }

    @Test
    @DisplayName("Deve limpar carrinho com sucesso")
    void clearCart_WhenHasItems_ShouldClearAllItems() throws Exception {
        // Primeiro adicionar um item
        AddToCartRequest request = AddToCartRequest.builder()
                .productId(testProduct1.getId())
                .quantity(2)
                .build();

        mockMvc.perform(post("/api/cart/{userId}/items", testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Limpar carrinho
        mockMvc.perform(delete("/api/cart/{userId}", testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Carrinho limpo com sucesso"));

        // Verificar se o carrinho está vazio
        mockMvc.perform(get("/api/cart/{userId}", testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalAmount", is(0)));
    }
}