package com.vogulev.regreso.controller;

import tools.jackson.databind.json.JsonMapper;
import com.vogulev.regreso.BaseIntegrationTest;
import com.vogulev.regreso.dto.request.ForgotPasswordRequest;
import com.vogulev.regreso.dto.request.LoginRequest;
import com.vogulev.regreso.dto.request.RegisterRequest;
import com.vogulev.regreso.dto.request.ResetPasswordRequest;
import com.vogulev.regreso.repository.BookingSettingsRepository;
import com.vogulev.regreso.repository.ClientRepository;
import com.vogulev.regreso.repository.PasswordResetCodeRepository;
import com.vogulev.regreso.repository.PractitionerRepository;
import com.vogulev.regreso.service.PasswordResetEmailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("AuthController — интеграционные тесты")
class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired JsonMapper objectMapper;
    @Autowired BookingSettingsRepository bookingSettingsRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired PasswordResetCodeRepository passwordResetCodeRepository;
    @Autowired PractitionerRepository practitionerRepository;
    @MockitoBean PasswordResetEmailService passwordResetEmailService;

    @BeforeEach
    void cleanUp() {
        reset(passwordResetEmailService);
        passwordResetCodeRepository.deleteAll();
        bookingSettingsRepository.deleteAll();
        clientRepository.deleteAll();
        practitionerRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        reset(passwordResetEmailService);
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

    @Nested
    @DisplayName("POST /api/auth/forgot-password")
    class ForgotPassword {

        @Test
        @DisplayName("существующий email → 200 и письмо с кодом")
        void existingEmail_shouldSendResetCode() throws Exception {
            registerUser("user@example.com", "password123");

            mockMvc.perform(post("/api/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(forgotPasswordRequest("user@example.com"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(containsString("мы отправили код")));

            ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> firstNameCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
            verify(passwordResetEmailService).sendResetCode(emailCaptor.capture(), firstNameCaptor.capture(), codeCaptor.capture());

            assertThat(emailCaptor.getValue()).isEqualTo("user@example.com");
            assertThat(firstNameCaptor.getValue()).isEqualTo("Тест");
            assertThat(codeCaptor.getValue()).matches("\\d{6}");
            assertThat(passwordResetCodeRepository.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("несуществующий email → 200 без отправки письма")
        void unknownEmail_shouldReturn200WithoutMail() throws Exception {
            mockMvc.perform(post("/api/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(forgotPasswordRequest("ghost@example.com"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(containsString("мы отправили код")));

            verify(passwordResetEmailService, never()).sendResetCode(anyString(), anyString(), anyString());
            assertThat(passwordResetCodeRepository.findAll()).isEmpty();
        }
    }

    @Nested
    @DisplayName("POST /api/auth/reset-password")
    class ResetPassword {

        @Test
        @DisplayName("валидный код → 200 и можно войти с новым паролем")
        void validCode_shouldResetPassword() throws Exception {
            registerUser("user@example.com", "password123");
            String code = requestResetCode("user@example.com");

            mockMvc.perform(post("/api/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(resetPasswordRequest("user@example.com", code, "newpassword123"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(containsString("успешно")));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(loginRequest("user@example.com", "newpassword123"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty());
        }

        @Test
        @DisplayName("неверный код → 400")
        void invalidCode_shouldReturn400() throws Exception {
            registerUser("user@example.com", "password123");
            requestResetCode("user@example.com");

            mockMvc.perform(post("/api/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(resetPasswordRequest("user@example.com", "000000", "newpassword123"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("Неверный код")));
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

    private ForgotPasswordRequest forgotPasswordRequest(String email) {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(email);
        return request;
    }

    private ResetPasswordRequest resetPasswordRequest(String email, String code, String newPassword) {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail(email);
        request.setCode(code);
        request.setNewPassword(newPassword);
        return request;
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

    private String requestResetCode(String email) throws Exception {
        reset(passwordResetEmailService);

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(forgotPasswordRequest(email))))
                .andExpect(status().isOk());

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordResetEmailService).sendResetCode(eq(email), anyString(), codeCaptor.capture());
        return codeCaptor.getValue();
    }

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}
