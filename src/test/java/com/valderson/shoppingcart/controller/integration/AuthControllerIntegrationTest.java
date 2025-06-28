package com.valderson.shoppingcart.controller.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valderson.shoppingcart.dto.request.LoginRequest;
import com.valderson.shoppingcart.dto.request.RegisterRequest;
import com.valderson.shoppingcart.entity.User;
import com.valderson.shoppingcart.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Deve registrar um novo usuário com sucesso")
    void testRegisterSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest("John Doe", "john@example.com", "123456");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(cookie().exists("authToken"))
                .andExpect(jsonPath("$.email").value("john@example.com"));

        // Verifica se usuário foi persistido no banco
        User savedUser = userRepository.findByEmail("john@example.com").orElse(null);
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Deve fazer login com sucesso e retornar token")
    void testLoginSuccess() throws Exception {
        // Criar usuário com senha encriptada
        User user = new User();
        user.setName("Jane Doe");
        user.setEmail("jane@example.com");
        user.setPasswordHash(passwordEncoder.encode("123456")); // senha correta

        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest("jane@example.com", "123456");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("authToken"))
                .andExpect(jsonPath("$.email").value("jane@example.com"));
    }

    @Test
    @DisplayName("Não deve registrar usuário com e-mail inválido")
    void testRegisterInvalidEmail() throws Exception {
        RegisterRequest request = new RegisterRequest("Invalid", "not-an-email", "123456");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}