package com.valderson.shoppingcart.controller.unit;

import com.valderson.shoppingcart.controller.AuthController;
import com.valderson.shoppingcart.dto.request.LoginRequest;
import com.valderson.shoppingcart.dto.request.RegisterRequest;
import com.valderson.shoppingcart.dto.response.UserResponse;
import com.valderson.shoppingcart.security.JwtTokenProvider;
import com.valderson.shoppingcart.service.AuthService;
import com.valderson.shoppingcart.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class AuthControllerTest {

    private AuthService authService;
    private JwtTokenProvider jwtTokenProvider;
    private CookieUtil cookieUtil;
    private AuthController authController;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        cookieUtil = mock(CookieUtil.class);
        authController = new AuthController(authService, jwtTokenProvider, cookieUtil);
    }

    @Test
    @DisplayName("Deve registrar usuário e retornar 201 com token no cookie")
    void testRegister() {
        RegisterRequest request = new RegisterRequest("Test", "test@example.com", "123456");
        UserResponse responseDto = UserResponse.builder()
                .id(1L)
                .name("Test")
                .email("test@example.com")
                .createdAt(LocalDateTime.now())
                .build();

        HttpServletResponse response = mock(HttpServletResponse.class);

        when(authService.register(request)).thenReturn(responseDto);
        when(jwtTokenProvider.generateToken(1L, "test@example.com")).thenReturn("mockedToken");

        ResponseEntity<UserResponse> responseEntity = authController.register(request, response);

        verify(cookieUtil).addAuthCookie(response, "mockedToken");
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(responseEntity.getBody())
                .usingRecursiveComparison()
                .isEqualTo(responseDto);
    }

    @Test
    @DisplayName("Deve logar usuário e retornar 200 com token no cookie")
    void testLogin() {
        LoginRequest request = new LoginRequest("test@example.com", "123456");
        UserResponse responseDto = UserResponse.builder()
                .id(1L)
                .name("Test")
                .email("test@example.com")
                .createdAt(LocalDateTime.now())
                .build();

        HttpServletResponse response = mock(HttpServletResponse.class);

        when(authService.login(request)).thenReturn(responseDto);
        when(jwtTokenProvider.generateToken(1L, "test@example.com")).thenReturn("mockedToken");

        ResponseEntity<UserResponse> responseEntity = authController.login(request, response);

        verify(cookieUtil).addAuthCookie(response, "mockedToken");
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody())
                .usingRecursiveComparison()
                .isEqualTo(responseDto);
    }

    @Test
    @DisplayName("Deve limpar cookie e retornar mensagem de logout")
    void testLogout() {
        HttpServletResponse response = mock(HttpServletResponse.class);

        ResponseEntity<String> responseEntity = authController.logout(response);

        verify(cookieUtil).clearAuthCookie(response);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isEqualTo("Logout realizado com sucesso");
    }

    @Test
    @DisplayName("Deve retornar usuário autenticado com sucesso")
    void testGetCurrentUser_withValidAttributes() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("userId")).thenReturn(1L);
        when(request.getAttribute("userEmail")).thenReturn("test@example.com");

        UserResponse responseDto = UserResponse.builder()
                .id(1L)
                .name("Test")
                .email("test@example.com")
                .createdAt(LocalDateTime.now())
                .build();

        when(authService.getUserById(1L)).thenReturn(responseDto);

        ResponseEntity<UserResponse> responseEntity = authController.getCurrentUser(request);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody())
                .usingRecursiveComparison()
                .isEqualTo(responseDto);
    }

    @Test
    @DisplayName("Deve retornar 401 se userId não estiver presente no request")
    void testGetCurrentUser_unauthorized() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("userId")).thenReturn(null);

        ResponseEntity<UserResponse> responseEntity = authController.getCurrentUser(request);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(responseEntity.getBody()).isNull();
    }
}
