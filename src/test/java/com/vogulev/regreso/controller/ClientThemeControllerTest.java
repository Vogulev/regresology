package com.vogulev.regreso.controller;

import tools.jackson.databind.json.JsonMapper;
import com.vogulev.regreso.BaseIntegrationTest;
import com.vogulev.regreso.dto.request.CreateSessionRequest;
import com.vogulev.regreso.dto.request.CreateThemeRequest;
import com.vogulev.regreso.dto.request.LinkSessionToThemeRequest;
import com.vogulev.regreso.dto.request.RegisterRequest;
import com.vogulev.regreso.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("ClientThemeController — интеграционные тесты")
class ClientThemeControllerTest extends BaseIntegrationTest {

    @Autowired JsonMapper objectMapper;
    @Autowired ReminderRepository reminderRepository;
    @Autowired ThemeSessionRepository themeSessionRepository;
    @Autowired ClientThemeRepository clientThemeRepository;
    @Autowired HomeworkRepository homeworkRepository;
    @Autowired SessionRepository sessionRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired PractitionerRepository practitionerRepository;

    @BeforeEach
    void cleanUp() {
        reminderRepository.deleteAll();
        themeSessionRepository.deleteAll();
        clientThemeRepository.deleteAll();
        homeworkRepository.deleteAll();
        sessionRepository.deleteAll();
        clientRepository.deleteAll();
        practitionerRepository.deleteAll();
    }

    // ── GET /api/clients/{clientId}/themes ────────────────────────────────────

    @Nested
    @DisplayName("GET /api/clients/{clientId}/themes")
    class GetClientThemes {

        @Test
        @DisplayName("без токена → 401")
        void noToken_returns401() throws Exception {
            mockMvc.perform(get("/api/clients/{id}/themes", UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("пустой список → 200 []")
        void empty_returns200() throws Exception {
            String token = registerAndGetToken("theme@test.com");
            String clientId = createClient(token);

            mockMvc.perform(get("/api/clients/{clientId}/themes", clientId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("чужой клиент → 404")
        void otherClient_returns404() throws Exception {
            String tokenA = registerAndGetToken("themeA@test.com");
            String tokenB = registerAndGetToken("themeB@test.com");
            String clientId = createClient(tokenA);

            mockMvc.perform(get("/api/clients/{clientId}/themes", clientId)
                            .header("Authorization", "Bearer " + tokenB))
                    .andExpect(status().isNotFound());
        }
    }

    // ── POST /api/clients/{clientId}/themes ───────────────────────────────────

    @Nested
    @DisplayName("POST /api/clients/{clientId}/themes")
    class CreateTheme {

        @Test
        @DisplayName("валидный запрос → 201 с темой")
        void valid_returns201() throws Exception {
            String token = registerAndGetToken("theme@test.com");
            String clientId = createClient(token);

            CreateThemeRequest req = new CreateThemeRequest();
            req.setTitle("Страх отвержения");
            req.setDescription("Паттерн из детства");

            mockMvc.perform(post("/api/clients/{clientId}/themes", clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.title").value("Страх отвержения"))
                    .andExpect(jsonPath("$.description").value("Паттерн из детства"))
                    .andExpect(jsonPath("$.isResolved").value(false))
                    .andExpect(jsonPath("$.sessionsCount").value(0))
                    .andExpect(jsonPath("$.sessions", hasSize(0)));
        }

        @Test
        @DisplayName("без title → 400")
        void missingTitle_returns400() throws Exception {
            String token = registerAndGetToken("theme@test.com");
            String clientId = createClient(token);

            mockMvc.perform(post("/api/clients/{clientId}/themes", clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"description\":\"Описание\"}")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("чужой клиент → 404")
        void otherClient_returns404() throws Exception {
            String tokenA = registerAndGetToken("themeA@test.com");
            String tokenB = registerAndGetToken("themeB@test.com");
            String clientId = createClient(tokenA);

            CreateThemeRequest req = new CreateThemeRequest();
            req.setTitle("Чужая тема");

            mockMvc.perform(post("/api/clients/{clientId}/themes", clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + tokenB))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("список возвращает созданную тему")
        void createdTheme_appearsInList() throws Exception {
            String token = registerAndGetToken("theme@test.com");
            String clientId = createClient(token);

            CreateThemeRequest req = new CreateThemeRequest();
            req.setTitle("Тревога");

            mockMvc.perform(post("/api/clients/{clientId}/themes", clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/api/clients/{clientId}/themes", clientId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].title").value("Тревога"));
        }
    }

    // ── POST /api/themes/{themeId}/sessions ───────────────────────────────────

    @Nested
    @DisplayName("POST /api/themes/{themeId}/sessions")
    class LinkSession {

        @Test
        @DisplayName("привязка сессии → 200, сессия появляется в теме")
        void linkSession_returns200() throws Exception {
            String token = registerAndGetToken("theme@test.com");
            String clientId = createClient(token);
            String sessionId = createSession(token, clientId);
            String themeId = createTheme(token, clientId, "Обида на мать");

            LinkSessionToThemeRequest req = new LinkSessionToThemeRequest();
            req.setSessionId(UUID.fromString(sessionId));
            req.setNotes("Ключевой момент сессии");

            mockMvc.perform(post("/api/themes/{themeId}/sessions", themeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionsCount").value(1))
                    .andExpect(jsonPath("$.sessions", hasSize(1)))
                    .andExpect(jsonPath("$.sessions[0].id").value(sessionId));
        }

        @Test
        @DisplayName("чужая тема → 404")
        void otherUserTheme_returns404() throws Exception {
            String tokenA = registerAndGetToken("themeA@test.com");
            String tokenB = registerAndGetToken("themeB@test.com");
            String clientId = createClient(tokenA);
            String sessionId = createSession(tokenA, clientId);
            String themeId = createTheme(tokenA, clientId, "Тема А");

            LinkSessionToThemeRequest req = new LinkSessionToThemeRequest();
            req.setSessionId(UUID.fromString(sessionId));

            mockMvc.perform(post("/api/themes/{themeId}/sessions", themeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + tokenB))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("чужая сессия → 404")
        void otherUserSession_returns404() throws Exception {
            String tokenA = registerAndGetToken("themeA@test.com");
            String tokenB = registerAndGetToken("themeB@test.com");
            String clientIdA = createClient(tokenA);
            String clientIdB = createClient(tokenB);
            String sessionIdB = createSession(tokenB, clientIdB);
            String themeId = createTheme(tokenA, clientIdA, "Тема А");

            LinkSessionToThemeRequest req = new LinkSessionToThemeRequest();
            req.setSessionId(UUID.fromString(sessionIdB));

            mockMvc.perform(post("/api/themes/{themeId}/sessions", themeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("повторная привязка той же сессии → 400")
        void duplicateLink_returns400() throws Exception {
            String token = registerAndGetToken("theme@test.com");
            String clientId = createClient(token);
            String sessionId = createSession(token, clientId);
            String themeId = createTheme(token, clientId, "Дублированная тема");

            LinkSessionToThemeRequest req = new LinkSessionToThemeRequest();
            req.setSessionId(UUID.fromString(sessionId));

            // Первый раз — успешно
            mockMvc.perform(post("/api/themes/{themeId}/sessions", themeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());

            // Второй раз — ошибка
            mockMvc.perform(post("/api/themes/{themeId}/sessions", themeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── PATCH /api/themes/{themeId}/resolve ───────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/themes/{themeId}/resolve")
    class ResolveTheme {

        @Test
        @DisplayName("resolve → 200, isResolved=true")
        void resolve_returns200() throws Exception {
            String token = registerAndGetToken("theme@test.com");
            String clientId = createClient(token);
            String themeId = createTheme(token, clientId, "Страх потери");

            mockMvc.perform(patch("/api/themes/{themeId}/resolve", themeId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isResolved").value(true))
                    .andExpect(jsonPath("$.resolvedAt").isNotEmpty());
        }

        @Test
        @DisplayName("повторный resolve → 400")
        void alreadyResolved_returns400() throws Exception {
            String token = registerAndGetToken("theme@test.com");
            String clientId = createClient(token);
            String themeId = createTheme(token, clientId, "Страх потери");

            mockMvc.perform(patch("/api/themes/{themeId}/resolve", themeId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());

            mockMvc.perform(patch("/api/themes/{themeId}/resolve", themeId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("чужая тема → 404")
        void otherUserTheme_returns404() throws Exception {
            String tokenA = registerAndGetToken("themeA@test.com");
            String tokenB = registerAndGetToken("themeB@test.com");
            String clientId = createClient(tokenA);
            String themeId = createTheme(tokenA, clientId, "Тема А");

            mockMvc.perform(patch("/api/themes/{themeId}/resolve", themeId)
                            .header("Authorization", "Bearer " + tokenB))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("несуществующая тема → 404")
        void notFound_returns404() throws Exception {
            String token = registerAndGetToken("theme@test.com");

            mockMvc.perform(patch("/api/themes/{themeId}/resolve", UUID.randomUUID())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String registerAndGetToken(String email) throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setPassword("password123");
        req.setFirstName("Тест");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String createClient(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Тест\",\"lastName\":\"Клиент\"}")
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createSession(String token, String clientId) throws Exception {
        CreateSessionRequest req = new CreateSessionRequest();
        req.setClientId(UUID.fromString(clientId));
        req.setScheduledAt(OffsetDateTime.of(2025, 6, 15, 10, 0, 0, 0, ZoneOffset.UTC));
        req.setDurationMin(90);
        req.setPrice(new BigDecimal("3000.00"));

        MvcResult result = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req))
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createTheme(String token, String clientId, String title) throws Exception {
        CreateThemeRequest req = new CreateThemeRequest();
        req.setTitle(title);

        MvcResult result = mockMvc.perform(post("/api/clients/{clientId}/themes", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req))
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}
