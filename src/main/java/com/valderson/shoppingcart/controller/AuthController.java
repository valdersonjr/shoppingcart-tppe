package com.valderson.shoppingcart.controller;

import com.valderson.shoppingcart.dto.request.LoginRequest;
import com.valderson.shoppingcart.dto.request.RegisterRequest;
import com.valderson.shoppingcart.dto.response.UserResponse;
import com.valderson.shoppingcart.security.JwtTokenProvider;
import com.valderson.shoppingcart.service.AuthService;
import com.valderson.shoppingcart.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtil cookieUtil;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletResponse response) {
        UserResponse user = authService.register(request);

        // Gerar token JWT
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());

        // Adicionar cookie HTTP-only
        cookieUtil.addAuthCookie(response, token);

        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletResponse response) {
        UserResponse user = authService.login(request);

        // Gerar token JWT
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());

        // Adicionar cookie HTTP-only
        cookieUtil.addAuthCookie(response, token);

        return ResponseEntity.ok(user);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletResponse response) {
        // Limpar cookie
        cookieUtil.clearAuthCookie(response);
        return ResponseEntity.ok("Logout realizado com sucesso");
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String email = (String) request.getAttribute("userEmail");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserResponse user = authService.getUserById(userId);
        return ResponseEntity.ok(user);
    }
}