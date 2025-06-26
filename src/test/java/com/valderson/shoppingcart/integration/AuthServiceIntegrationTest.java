package com.valderson.shoppingcart.integration;

import com.valderson.shoppingcart.dto.request.LoginRequest;
import com.valderson.shoppingcart.dto.request.RegisterRequest;
import com.valderson.shoppingcart.dto.response.UserResponse;
import com.valderson.shoppingcart.entity.User;
import com.valderson.shoppingcart.repository.UserRepository;
import com.valderson.shoppingcart.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=password",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
@DisplayName("AuthService - Testes de Integração")
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Limpar dados antes de cada teste
        userRepository.deleteAll();
    }

    // ========================
    // TESTES DE INTEGRAÇÃO
    // ========================

    @Test
    @DisplayName("Deve registrar e fazer login com usuário real")
    void shouldRegisterAndLoginWithRealUser() {
        // Given - Registro
        RegisterRequest registerRequest = RegisterRequest.builder()
                .name("João Integration")
                .email("joao.integration@email.com")
                .password("senha123")
                .build();

        // When - Registrar usuário
        UserResponse registeredUser = authService.register(registerRequest);

        // Then - Verificar registro
        assertThat(registeredUser).isNotNull();
        assertThat(registeredUser.getId()).isNotNull();
        assertThat(registeredUser.getName()).isEqualTo("João Integration");
        assertThat(registeredUser.getEmail()).isEqualTo("joao.integration@email.com");
        assertThat(registeredUser.getCreatedAt()).isNotNull();

        // Verificar se foi salvo no banco
        User savedUser = userRepository.findById(registeredUser.getId()).orElse(null);
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getName()).isEqualTo("João Integration");
        assertThat(savedUser.getEmail()).isEqualTo("joao.integration@email.com");
        assertThat(passwordEncoder.matches("senha123", savedUser.getPasswordHash())).isTrue();

        // Given - Login
        LoginRequest loginRequest = LoginRequest.builder()
                .email("joao.integration@email.com")
                .password("senha123")
                .build();

        // When - Fazer login
        UserResponse loggedUser = authService.login(loginRequest);

        // Then - Verificar login
        assertThat(loggedUser).isNotNull();
        assertThat(loggedUser.getId()).isEqualTo(registeredUser.getId());
        assertThat(loggedUser.getName()).isEqualTo("João Integration");
        assertThat(loggedUser.getEmail()).isEqualTo("joao.integration@email.com");
    }

    @Test
    @DisplayName("Deve impedir registro com email duplicado")
    void shouldPreventDuplicateEmailRegistration() {
        // Given - Primeiro usuário
        RegisterRequest firstUser = RegisterRequest.builder()
                .name("Primeiro Usuário")
                .email("duplicado@email.com")
                .password("senha123")
                .build();

        // When - Registrar primeiro usuário
        UserResponse firstRegistered = authService.register(firstUser);
        assertThat(firstRegistered).isNotNull();

        // Given - Segundo usuário com mesmo email
        RegisterRequest secondUser = RegisterRequest.builder()
                .name("Segundo Usuário")
                .email("duplicado@email.com") // Email duplicado
                .password("outraSenha456")
                .build();

        // When & Then - Tentar registrar segundo usuário
        assertThatThrownBy(() -> authService.register(secondUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email já está em uso");

        // Verificar que só existe um usuário no banco
        long userCount = userRepository.count();
        assertThat(userCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve falhar login com senha incorreta")
    void shouldFailLoginWithWrongPassword() {
        // Given - Registrar usuário
        RegisterRequest registerRequest = RegisterRequest.builder()
                .name("Usuário Teste")
                .email("teste.senha@email.com")
                .password("senhaCorreta123")
                .build();

        authService.register(registerRequest);

        // Given - Login com senha errada
        LoginRequest wrongPasswordLogin = LoginRequest.builder()
                .email("teste.senha@email.com")
                .password("senhaErrada456")
                .build();

        // When & Then
        assertThatThrownBy(() -> authService.login(wrongPasswordLogin))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Senha incorreta");
    }

    @Test
    @DisplayName("Deve buscar usuário por ID após registro")
    void shouldGetUserByIdAfterRegistration() {
        // Given
        RegisterRequest registerRequest = RegisterRequest.builder()
                .name("Usuário Para Busca")
                .email("busca@email.com")
                .password("senha123")
                .build();

        // When
        UserResponse registeredUser = authService.register(registerRequest);
        UserResponse foundUser = authService.getUserById(registeredUser.getId());

        // Then
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getId()).isEqualTo(registeredUser.getId());
        assertThat(foundUser.getName()).isEqualTo("Usuário Para Busca");
        assertThat(foundUser.getEmail()).isEqualTo("busca@email.com");
        assertThat(foundUser.getCreatedAt()).isEqualTo(registeredUser.getCreatedAt());
    }

    @Test
    @DisplayName("Deve falhar ao buscar usuário inexistente")
    void shouldFailWhenSearchingNonExistentUser() {
        // Given
        Long nonExistentId = 999L;

        // When & Then
        assertThatThrownBy(() -> authService.getUserById(nonExistentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Usuário não encontrado");
    }

    // ====================================
    // TESTES PARAMETRIZADOS DE INTEGRAÇÃO
    // ====================================

    @ParameterizedTest
    @DisplayName("Deve registrar múltiplos usuários com dados diferentes")
    @CsvSource({
            "Maria Silva, maria.silva@email.com, senha123",
            "João Santos, joao.santos@email.com, minhaSenh@456",
            "Ana Costa, ana.costa@email.com, password789",
            "Carlos Oliveira, carlos.oliveira@email.com, secret2024"
    })
    void shouldRegisterMultipleUsersWithDifferentData(String name, String email, String password) {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .name(name)
                .email(email)
                .password(password)
                .build();

        // When
        UserResponse response = authService.register(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo(name);
        assertThat(response.getEmail()).isEqualTo(email);
        assertThat(response.getId()).isNotNull();
        assertThat(response.getCreatedAt()).isNotNull();

        // Verificar no banco
        User savedUser = userRepository.findById(response.getId()).orElse(null);
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getName()).isEqualTo(name);
        assertThat(savedUser.getEmail()).isEqualTo(email);

        // Verificar senha foi criptografada
        assertThat(passwordEncoder.matches(password, savedUser.getPasswordHash())).isTrue();

        // Teste de login para garantir integração completa
        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        UserResponse loginResponse = authService.login(loginRequest);
        assertThat(loginResponse.getId()).isEqualTo(response.getId());
    }

    @Test
    @DisplayName("Deve testar fluxo completo: registro -> login -> busca por ID")
    void shouldTestCompleteFlow() {
        // Given
        RegisterRequest registerRequest = RegisterRequest.builder()
                .name("Fluxo Completo")
                .email("fluxo.completo@email.com")
                .password("senhaCompleta123")
                .build();

        // When - Registro
        UserResponse registeredUser = authService.register(registerRequest);

        // Then - Verificar registro
        assertThat(registeredUser.getId()).isNotNull();

        // When - Login
        LoginRequest loginRequest = LoginRequest.builder()
                .email("fluxo.completo@email.com")
                .password("senhaCompleta123")
                .build();

        UserResponse loggedUser = authService.login(loginRequest);

        // Then - Verificar login
        assertThat(loggedUser.getId()).isEqualTo(registeredUser.getId());

        // When - Buscar por ID
        UserResponse foundUser = authService.getUserById(registeredUser.getId());

        // Then - Verificar busca
        assertThat(foundUser.getId()).isEqualTo(registeredUser.getId());
        assertThat(foundUser.getName()).isEqualTo("Fluxo Completo");
        assertThat(foundUser.getEmail()).isEqualTo("fluxo.completo@email.com");

        // Verificar consistência entre todas as operações
        assertThat(registeredUser.getId()).isEqualTo(loggedUser.getId()).isEqualTo(foundUser.getId());
        assertThat(registeredUser.getName()).isEqualTo(loggedUser.getName()).isEqualTo(foundUser.getName());
        assertThat(registeredUser.getEmail()).isEqualTo(loggedUser.getEmail()).isEqualTo(foundUser.getEmail());
    }
}