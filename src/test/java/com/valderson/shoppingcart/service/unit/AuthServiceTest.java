package com.valderson.shoppingcart.service.unit;

import com.valderson.shoppingcart.dto.request.LoginRequest;
import com.valderson.shoppingcart.dto.request.RegisterRequest;
import com.valderson.shoppingcart.dto.response.UserResponse;
import com.valderson.shoppingcart.entity.User;
import com.valderson.shoppingcart.repository.UserRepository;
import com.valderson.shoppingcart.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - Testes Unitários")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .name("João Silva")
                .email("joao@email.com")
                .passwordHash("hashedPassword123")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Deve registrar usuário com sucesso")
    void shouldRegisterUserSuccessfully() {
        RegisterRequest request = RegisterRequest.builder()
                .name("João Silva")
                .email("joao@email.com")
                .password("senha123")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hashedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        UserResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("João Silva");
        assertThat(response.getEmail()).isEqualTo("joao@email.com");

        verify(userRepository).existsByEmail("joao@email.com");
        verify(passwordEncoder).encode("senha123");
        verify(userRepository).save(any(User.class));
    }

    @ParameterizedTest
    @CsvSource({
            "João Silva, joao@email.com, senha123",
            "Maria Santos, maria@email.com, senha456",
            "Pedro Oliveira, pedro@email.com, senha789",
            "Ana Costa, ana@email.com, senhaABC"
    })
    @DisplayName("Deve registrar diferentes usuários com sucesso")
    void shouldRegisterUsersWithDifferentValidData(String name, String email, String password) {
        RegisterRequest request = RegisterRequest.builder()
                .name(name)
                .email(email)
                .password(password)
                .build();

        User user = User.builder()
                .id(1L)
                .name(name)
                .email(email)
                .passwordHash("hashedPassword")
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo(name);
        assertThat(response.getEmail()).isEqualTo(email);

        verify(userRepository).existsByEmail(email);
        verify(passwordEncoder).encode(password);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "email-invalido", "@email.com", "email@", "email.com"})
    @DisplayName("Deve validar diferentes emails inválidos no registro")
    void shouldValidateInvalidEmailsInRegistration(String invalidEmail) {
        RegisterRequest request = RegisterRequest.builder()
                .name("João Silva")
                .email(invalidEmail)
                .password("senha123")
                .build();

        when(userRepository.existsByEmail(invalidEmail)).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("hashedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        assertThatNoException().isThrownBy(() -> authService.register(request));
    }

    @Test
    @DisplayName("Deve lançar exceção quando email já existe no registro")
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        RegisterRequest request = RegisterRequest.builder()
                .name("João Silva")
                .email("joao@email.com")
                .password("senha123")
                .build();

        when(userRepository.existsByEmail("joao@email.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email já está em uso");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Deve fazer login com sucesso")
    void shouldLoginSuccessfully() {
        LoginRequest request = LoginRequest.builder()
                .email("joao@email.com")
                .password("senha123")
                .build();

        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("senha123", "hashedPassword123")).thenReturn(true);

        UserResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("joao@email.com");

        verify(userRepository).findByEmail("joao@email.com");
        verify(passwordEncoder).matches("senha123", "hashedPassword123");
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário não encontrado no login")
    void shouldThrowExceptionWhenUserNotFoundInLogin() {
        LoginRequest request = LoginRequest.builder()
                .email("inexistente@email.com")
                .password("qualquer")
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Usuário não encontrado");

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("Deve lançar exceção quando senha incorreta")
    void shouldThrowExceptionWhenPasswordIsIncorrect() {
        LoginRequest request = LoginRequest.builder()
                .email("joao@email.com")
                .password("senhaErrada")
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("senhaErrada", "hashedPassword123")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Senha incorreta");
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 2L, 3L, 100L, 999L})
    @DisplayName("Deve buscar usuários por diferentes IDs com sucesso")
    void shouldGetUserByDifferentIds(Long userId) {
        User user = User.builder()
                .id(userId)
                .name("Usuário " + userId)
                .email("user" + userId + "@email.com")
                .passwordHash("hashedPassword")
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse response = authService.getUserById(userId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(userId);
        assertThat(response.getName()).isEqualTo("Usuário " + userId);
        assertThat(response.getEmail()).isEqualTo("user" + userId + "@email.com");

        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário não encontrado por ID")
    void shouldThrowExceptionWhenUserNotFoundById() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getUserById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Usuário não encontrado");
    }
}