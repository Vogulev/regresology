package com.vogulev.regreso.service;

import com.vogulev.regreso.dto.request.LoginRequest;
import com.vogulev.regreso.dto.request.RegisterRequest;
import com.vogulev.regreso.dto.response.AuthResponse;

/**
 * Сервис аутентификации и управления сессиями пользователей.
 * Отвечает за регистрацию, вход и обновление токенов доступа.
 */
public interface AuthService {

    /**
     * Регистрирует нового практика в системе.
     *
     * @param request данные для регистрации (имя, email, пароль)
     * @return ответ с access- и refresh-токенами
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Выполняет вход практика по email и паролю.
     *
     * @param request данные для входа (email, пароль)
     * @return ответ с access- и refresh-токенами
     */
    AuthResponse login(LoginRequest request);

    /**
     * Обновляет access-токен по действующему refresh-токену.
     *
     * @param refreshToken действующий refresh-токен
     * @return ответ с новыми access- и refresh-токенами
     */
    AuthResponse refresh(String refreshToken);
}
