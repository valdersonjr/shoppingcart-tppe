package com.valderson.shoppingcart.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtConfig {
    private String secret = "mySecretKey123456789012345678901234567890"; // Mínimo 32 caracteres
    private int expiration = 86400; // 24 horas em segundos
    private String cookieName = "authToken";
}