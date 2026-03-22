package com.vogulev.regreso.controller;

import tools.jackson.databind.json.JsonMapper;
import com.vogulev.regreso.BaseIntegrationTest;
import com.vogulev.regreso.dto.request.LoginRequest;
import com.vogulev.regreso.dto.request.RegisterRequest;
import com.vogulev.regreso.repository.BookingSettingsRepository;
import com.vogulev.regreso.repository.ClientRepository;
import com.vogulev.regreso.repository.PractitionerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("AuthController — интеграционные тесты")
class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired JsonMapper objectMapper;
    @Autowired BookingSettingsRepository bookingSettingsRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired PractitionerRepository practitionerRepository;

    @BeforeEach
    void cleanUp() {
        bookingSettingsRepository.deleteAll();
        clientRepository.deleteAll();
        practitionerRepository.deleteAll();
    }

    // ── POST /api/auth/register ───────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/auth/register")
    class Register {

        @Test
        @DisplayName("валидные данные → 201 с access и refresh токенами")
        void validRequest_shouldReturn201WithTokens() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(validRegisterRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"));
        }

        @Test
        @DisplayName("дублирующийся email → 400")
        void duplicateEmail_shouldReturn400() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(validRegisterRequest()))).andReturn();

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(validRegisterRequest())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("Email")));
        }

        @Test
        @DisplayName("невалидный email → 400")
        void invalidEmail_shouldReturn400() throws Exception {
            RegisterRequest req = validRegisterRequest();
            req.setEmail("not-an-email");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.email").exists());
        }

        @Test
        @DisplayName("пароль короче 8 символов → 400")
        void shortPassword_shouldReturn400() throws Exception {
            RegisterRequest req = validRegisterRequest();
            req.setPassword("short");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.password").exists());
        }

        @Test
        @DisplayName("пустое имя → 400")
        void blankFirstName_shouldReturn400() throws Exception {
            RegisterRequest req = validRegisterRequest();
            req.setFirstName("");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.firstName").exists());
        }

        @Test
        @DisplayName("пустое тело → 400")
        void emptyBody_shouldReturn400() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── POST /api/auth/login ──────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("верные данные → 200 с токенами")
        void validCredentials_shouldReturn200WithTokens() throws Exception {
            registerUser("user@example.com", "password123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(loginRequest("user@example.com", "password123"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty());
        }

        @Test
        @DisplayName("неверный пароль → 401")
        void wrongPassword_shouldReturn401() throws Exception {
            registerUser("user@example.com", "password123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(loginRequest("user@example.com", "wrongpass"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").isNotEmpty());
        }

        @Test
        @DisplayName("несуществующий пользователь → 401")
        void unknownUser_shouldReturn401() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(loginRequest("ghost@example.com", "password123"))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("пустой email → 400")
        void blankEmail_shouldReturn400() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(loginRequest("", "password123"))))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── POST /api/auth/refresh ────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class Refresh {

        @Test
        @DisplayName("валидный refresh-токен → 200 с новой парой токенов")
        void validRefreshToken_shouldReturn200WithNewTokens() throws Exception {
            String refreshToken = registerAndGetRefreshToken("user@example.com");

            mockMvc.perform(post("/api/auth/refresh")
                            .header("X-Refresh-Token", refreshToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty());
        }

        @Test
        @DisplayName("access-токен вместо refresh → 400")
        void accessTokenAsRefresh_shouldReturn400() throws Exception {
            String accessToken = registerAndGetAccessToken("user@example.com");

            mockMvc.perform(post("/api/auth/refresh")
                            .header("X-Refresh-Token", accessToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("refresh token")));
        }

        @Test
        @DisplayName("случайная строка вместо токена → 401")
        void garbageToken_shouldReturn401() throws Exception {
            mockMvc.perform(post("/api/auth/refresh")
                            .header("X-Refresh-Token", "not.a.jwt"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── JWT-фильтр ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("JWT-фильтр на защищённых маршрутах")
    class JwtFilter {

        @Test
        @DisplayName("запрос без токена на защищённый маршрут → 401")
        void noToken_onProtectedRoute_shouldReturn401() throws Exception {
            mockMvc.perform(get("/api/clients"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("невалидный токен на защищённом маршруте → 401")
        void invalidToken_onProtectedRoute_shouldReturn401() throws Exception {
            mockMvc.perform(get("/api/clients")
                            .header("Authorization", "Bearer garbage.token.value"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("валидный access-токен — фильтр пропускает запрос дальше")
        void validToken_shouldPassFilter() throws Exception {
            String accessToken = registerAndGetAccessToken("filter@example.com");

            // /api/clients реализован → 200, не 401
            mockMvc.perform(get("/api/clients")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk());
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private RegisterRequest validRegisterRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@example.com");
        req.setPassword("password123");
        req.setFirstName("Иван");
        req.setLastName("Иванов");
        return req;
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }

    private void registerUser(String email, String password) throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setPassword(password);
        req.setFirstName("Тест");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(req)));
    }

    private String registerAndGetAccessToken(String email) throws Exception {
        return extractToken(email, "accessToken");
    }

    private String registerAndGetRefreshToken(String email) throws Exception {
        return extractToken(email, "refreshToken");
    }

    private String extractToken(String email, String tokenField) throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setPassword("password123");
        req.setFirstName("Тест");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        return objectMapper.readTree(body).get(tokenField).asText();
    }

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}
