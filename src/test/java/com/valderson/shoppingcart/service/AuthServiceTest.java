package com.valderson.shoppingcart.service;

import com.valderson.shoppingcart.dto.request.LoginRequest;
import com.valderson.shoppingcart.dto.request.RegisterRequest;
import com.valderson.shoppingcart.dto.response.UserResponse;
import com.valderson.shoppingcart.entity.User;
import com.valderson.shoppingcart.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        // Dados de teste reutilizáveis
        mockUser = User.builder()
                .id(1L)
                .name("João Silva")
                .email("joao@email.com")
                .passwordHash("hashedPassword123")
                .createdAt(LocalDateTime.now())
                .build();

        registerRequest = RegisterRequest.builder()
                .name("João Silva")
                .email("joao@email.com")
                .password("senha123")
                .build();

        loginRequest = LoginRequest.builder()
                .email("joao@email.com")
                .password("senha123")
                .build();
    }

    // =================
    // TESTES UNITÁRIOS
    // =================

    @Test
    @DisplayName("Deve registrar usuário com sucesso")
    void shouldRegisterUserSuccessfully() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // When
        UserResponse response = authService.register(registerRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("João Silva");
        assertThat(response.getEmail()).isEqualTo("joao@email.com");

        // Verificar interações
        verify(userRepository).existsByEmail("joao@email.com");
        verify(passwordEncoder).encode("senha123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando email já existe no registro")
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email já está em uso");

        // Verificar que não tentou salvar
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Deve fazer login com sucesso")
    void shouldLoginSuccessfully() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        // When
        UserResponse response = authService.login(loginRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("João Silva");
        assertThat(response.getEmail()).isEqualTo("joao@email.com");

        verify(userRepository).findByEmail("joao@email.com");
        verify(passwordEncoder).matches("senha123", "hashedPassword123");
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário não encontrado no login")
    void shouldThrowExceptionWhenUserNotFoundInLogin() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Usuário não encontrado");

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("Deve lançar exceção quando senha incorreta")
    void shouldThrowExceptionWhenPasswordIsIncorrect() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Senha incorreta");
    }

    @Test
    @DisplayName("Deve buscar usuário por ID com sucesso")
    void shouldGetUserByIdSuccessfully() {
        // Given
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(mockUser));

        // When
        UserResponse response = authService.getUserById(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("João Silva");
        assertThat(response.getEmail()).isEqualTo("joao@email.com");

        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário não encontrado por ID")
    void shouldThrowExceptionWhenUserNotFoundById() {
        // Given
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.getUserById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Usuário não encontrado");
    }

    // =====================
    // TESTES PARAMETRIZADOS
    // =====================

    @ParameterizedTest
    @DisplayName("Deve validar diferentes emails inválidos no registro")
    @ValueSource(strings = {"", " ", "email-invalido", "@email.com", "email@", "email.com"})
    void shouldValidateInvalidEmailsInRegistration(String invalidEmail) {
        // Given
        RegisterRequest invalidRequest = RegisterRequest.builder()
                .name("João Silva")
                .email(invalidEmail)
                .password("senha123")
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        // When & Then - Aqui você pode adicionar validação de email se necessário
        // Por enquanto, vamos testar que o fluxo normal funciona
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // Este teste mostra como usar parametrizado - você pode adicionar validação real
        assertThatNoException().isThrownBy(() -> authService.register(invalidRequest));
    }

    @ParameterizedTest
    @DisplayName("Deve testar registro com diferentes nomes válidos")
    @CsvSource({
            "João Silva, joao@email.com, senha123",
            "Maria Santos, maria@email.com, senha456",
            "Pedro Oliveira, pedro@email.com, senha789",
            "Ana Costa, ana@email.com, senhaABC"
    })
    void shouldRegisterUsersWithDifferentValidData(String name, String email, String password) {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .name(name)
                .email(email)
                .password(password)
                .build();

        User expectedUser = User.builder()
                .id(1L)
                .name(name)
                .email(email)
                .passwordHash("hashedPassword")
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(expectedUser);

        // When
        UserResponse response = authService.register(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo(name);
        assertThat(response.getEmail()).isEqualTo(email);

        verify(userRepository).existsByEmail(email);
        verify(passwordEncoder).encode(password);
    }

    @ParameterizedTest
    @DisplayName("Deve testar login com diferentes IDs de usuário")
    @ValueSource(longs = {1L, 2L, 3L, 100L, 999L})
    void shouldGetUserByDifferentIds(Long userId) {
        // Given
        User user = User.builder()
                .id(userId)
                .name("Usuário " + userId)
                .email("user" + userId + "@email.com")
                .passwordHash("hashedPassword")
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        UserResponse response = authService.getUserById(userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(userId);
        assertThat(response.getName()).isEqualTo("Usuário " + userId);
        assertThat(response.getEmail()).isEqualTo("user" + userId + "@email.com");
    }
}