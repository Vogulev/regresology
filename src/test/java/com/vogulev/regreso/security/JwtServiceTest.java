package com.vogulev.regreso.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService")
class JwtServiceTest {

    private static final String EMAIL = "test@example.com";
    private static final String SECRET = "test-secret-key-for-tests-minimum-32-chars-long";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 86400000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpirationMs", 604800000L);
    }

    @Test
    @DisplayName("generateAccessToken — возвращает непустую строку")
    void generateAccessToken_shouldReturnNonBlankToken() {
        String token = jwtService.generateAccessToken(EMAIL);
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("generateRefreshToken — возвращает непустую строку")
    void generateRefreshToken_shouldReturnNonBlankToken() {
        String token = jwtService.generateRefreshToken(EMAIL);
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("extractEmail — возвращает email из access-токена")
    void extractEmail_fromAccessToken_shouldReturnCorrectEmail() {
        String token = jwtService.generateAccessToken(EMAIL);
        assertThat(jwtService.extractEmail(token)).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("extractEmail — возвращает email из refresh-токена")
    void extractEmail_fromRefreshToken_shouldReturnCorrectEmail() {
        String token = jwtService.generateRefreshToken(EMAIL);
        assertThat(jwtService.extractEmail(token)).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("access-токен не является refresh-токеном")
    void isRefreshToken_withAccessToken_shouldReturnFalse() {
        String token = jwtService.generateAccessToken(EMAIL);
        assertThat(jwtService.isRefreshToken(token)).isFalse();
    }

    @Test
    @DisplayName("refresh-токен определяется корректно")
    void isRefreshToken_withRefreshToken_shouldReturnTrue() {
        String token = jwtService.generateRefreshToken(EMAIL);
        assertThat(jwtService.isRefreshToken(token)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid — валидный токен и совпадающий пользователь")
    void isTokenValid_withMatchingUser_shouldReturnTrue() {
        String token = jwtService.generateAccessToken(EMAIL);
        UserDetails userDetails = buildUser(EMAIL);
        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid — токен не принадлежит этому пользователю")
    void isTokenValid_withDifferentUser_shouldReturnFalse() {
        String token = jwtService.generateAccessToken(EMAIL);
        UserDetails otherUser = buildUser("other@example.com");
        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    @DisplayName("access и refresh токены — это разные строки")
    void accessAndRefreshTokens_shouldBeDifferent() {
        String access = jwtService.generateAccessToken(EMAIL);
        String refresh = jwtService.generateRefreshToken(EMAIL);
        assertThat(access).isNotEqualTo(refresh);
    }

    private UserDetails buildUser(String email) {
        return User.withUsername(email).password("pass").authorities(List.of()).build();
    }
}
