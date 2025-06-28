package com.valderson.shoppingcart.service.integration;

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
        userRepository.deleteAll();
    }

    @ParameterizedTest
    @CsvSource({
            "João Integration, joao.integration@email.com, senha123",
            "Maria Silva, maria@email.com, abc123456",
            "Ana Teste, ana@email.com, securePass",
            "Carlos Example, carlos@email.com, senhaSegura!"
    })
    @DisplayName("Deve registrar, logar e buscar usuário com sucesso usando diferentes dados")
    void shouldRegisterLoginAndSearchWithDifferentUsers(String name, String email, String password) {
        // Registro
        RegisterRequest registerRequest = RegisterRequest.builder()
                .name(name)
                .email(email)
                .password(password)
                .build();

        UserResponse registeredUser = authService.register(registerRequest);

        assertThat(registeredUser).isNotNull();
        assertThat(registeredUser.getId()).isNotNull();
        assertThat(registeredUser.getName()).isEqualTo(name);
        assertThat(registeredUser.getEmail()).isEqualTo(email);
        assertThat(registeredUser.getCreatedAt()).isNotNull();

        // Validação no banco
        User savedUser = userRepository.findById(registeredUser.getId()).orElse(null);
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getName()).isEqualTo(name);
        assertThat(savedUser.getEmail()).isEqualTo(email);
        assertThat(passwordEncoder.matches(password, savedUser.getPasswordHash())).isTrue();

        // Login
        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        UserResponse loggedUser = authService.login(loginRequest);

        assertThat(loggedUser.getId()).isEqualTo(registeredUser.getId());
        assertThat(loggedUser.getName()).isEqualTo(name);
        assertThat(loggedUser.getEmail()).isEqualTo(email);

        // Buscar por ID
        UserResponse foundUser = authService.getUserById(registeredUser.getId());

        assertThat(foundUser.getId()).isEqualTo(registeredUser.getId());
        assertThat(foundUser.getName()).isEqualTo(name);
        assertThat(foundUser.getEmail()).isEqualTo(email);
        assertThat(foundUser.getCreatedAt()).isEqualTo(registeredUser.getCreatedAt());
    }

    @Test
    @DisplayName("Deve impedir registro com email duplicado")
    void shouldPreventDuplicateEmailRegistration() {
        RegisterRequest firstUser = RegisterRequest.builder()
                .name("Primeiro Usuário")
                .email("duplicado@email.com")
                .password("senha123")
                .build();

        RegisterRequest secondUser = RegisterRequest.builder()
                .name("Segundo Usuário")
                .email("duplicado@email.com")
                .password("outraSenha456")
                .build();

        UserResponse registered = authService.register(firstUser);
        assertThat(registered).isNotNull();

        assertThatThrownBy(() -> authService.register(secondUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email já está em uso");

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve falhar login com senha incorreta")
    void shouldFailLoginWithWrongPassword() {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .name("Usuário Teste")
                .email("teste.senha@email.com")
                .password("senhaCorreta123")
                .build();

        authService.register(registerRequest);

        LoginRequest wrongLogin = LoginRequest.builder()
                .email("teste.senha@email.com")
                .password("senhaErrada456")
                .build();

        assertThatThrownBy(() -> authService.login(wrongLogin))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Senha incorreta");
    }

    @Test
    @DisplayName("Deve falhar ao buscar usuário inexistente")
    void shouldFailWhenSearchingNonExistentUser() {
        assertThatThrownBy(() -> authService.getUserById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Usuário não encontrado");
    }
}