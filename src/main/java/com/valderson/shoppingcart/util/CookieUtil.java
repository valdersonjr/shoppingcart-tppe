package com.valderson.shoppingcart.util;

import com.valderson.shoppingcart.config.JwtConfig;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CookieUtil {

    private final JwtConfig jwtConfig;

    public void addAuthCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(jwtConfig.getCookieName(), token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // true em produção com HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(jwtConfig.getExpiration());
        response.addCookie(cookie);
    }

    public void clearAuthCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(jwtConfig.getCookieName(), "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // true em produção com HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(0); // Remove o cookie
        response.addCookie(cookie);
    }
}