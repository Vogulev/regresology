package com.vogulev.regreso.controller;

import tools.jackson.databind.json.JsonMapper;
import com.vogulev.regreso.BaseIntegrationTest;
import com.vogulev.regreso.dto.request.CreateSessionRequest;
import com.vogulev.regreso.dto.request.RegisterRequest;
import com.vogulev.regreso.entity.Client;
import com.vogulev.regreso.entity.Session;
import com.vogulev.regreso.repository.BookingSettingsRepository;
import com.vogulev.regreso.repository.ClientRepository;
import com.vogulev.regreso.repository.PractitionerRepository;
import com.vogulev.regreso.repository.ReminderRepository;
import com.vogulev.regreso.repository.SessionRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("SessionSummaryController — интеграционные тесты (ai.provider=stub)")
class SessionSummaryControllerTest extends BaseIntegrationTest {

    @Autowired JsonMapper objectMapper;
    @Autowired BookingSettingsRepository bookingSettingsRepository;
    @Autowired SessionRepository sessionRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired PractitionerRepository practitionerRepository;
    @Autowired ReminderRepository reminderRepository;

    @BeforeEach
    void cleanUp() {
        bookingSettingsRepository.deleteAll();
        reminderRepository.deleteAll();
        sessionRepository.deleteAll();
        clientRepository.deleteAll();
        practitionerRepository.deleteAll();
    }

    // ── GET /api/sessions/{id}/summary ────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/sessions/{id}/summary")
    class GetSessionSummary {

        @Test
        @DisplayName("ai_summary = null → 200 PENDING")
        void nullSummary_returnsPending() throws Exception {
            String token = registerAndGetToken("summary@test.com");
            String clientId = createClient(token);
            String sessionId = createSession(token, clientId);

            mockMvc.perform(get("/api/sessions/{id}/summary", sessionId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value(sessionId))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.summary").doesNotExist());
        }

        @Test
        @DisplayName("ai_summary заполнен → 200 READY с текстом")
        void filledSummary_returnsReady() throws Exception {
            String token = registerAndGetToken("summary@test.com");
            String clientId = createClient(token);
            String sessionId = createSession(token, clientId);

            // Напрямую обновляем ai_summary в репозитории
            Session session = sessionRepository.findById(UUID.fromString(sessionId)).orElseThrow();
            session.setAiSummary("Тестовое саммари");
            session.setAiSummaryGeneratedAt(OffsetDateTime.now().minusHours(2));
            sessionRepository.save(session);

            mockMvc.perform(get("/api/sessions/{id}/summary", sessionId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value(sessionId))
                    .andExpect(jsonPath("$.status").value("READY"))
                    .andExpect(jsonPath("$.summary").value("Тестовое саммари"))
                    .andExpect(jsonPath("$.generatedAt").isNotEmpty());
        }

        @Test
        @DisplayName("без токена → 401")
        void noToken_returns401() throws Exception {
            mockMvc.perform(get("/api/sessions/{id}/summary", UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("чужая сессия → 404")
        void otherUserSession_returns404() throws Exception {
            String tokenA = registerAndGetToken("summaryA@test.com");
            String tokenB = registerAndGetToken("summaryB@test.com");
            String clientId = createClient(tokenA);
            String sessionId = createSession(tokenA, clientId);

            mockMvc.perform(get("/api/sessions/{id}/summary", sessionId)
                            .header("Authorization", "Bearer " + tokenB))
                    .andExpect(status().isNotFound());
        }
    }

    // ── POST /api/sessions/{id}/summary/generate ──────────────────────────────

    @Nested
    @DisplayName("POST /api/sessions/{id}/summary/generate")
    class TriggerSessionSummary {

        @Test
        @DisplayName("первый запрос → 202 Accepted")
        void firstRequest_returns202() throws Exception {
            String token = registerAndGetToken("summary@test.com");
            String clientId = createClient(token);
            String sessionId = createSession(token, clientId);

            mockMvc.perform(post("/api/sessions/{id}/summary/generate", sessionId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isAccepted());
        }

        @Test
        @DisplayName("повторный запрос в cooldown → 429")
        void repeatWithinCooldown_returns429() throws Exception {
            String token = registerAndGetToken("summary@test.com");
            String clientId = createClient(token);
            String sessionId = createSession(token, clientId);

            // Имитируем уже сгенерированное саммари (30 мин назад — в cooldown)
            Session session = sessionRepository.findById(UUID.fromString(sessionId)).orElseThrow();
            session.setAiSummary("Предыдущее саммари");
            session.setAiSummaryGeneratedAt(OffsetDateTime.now().minusMinutes(30));
            sessionRepository.save(session);

            mockMvc.perform(post("/api/sessions/{id}/summary/generate", sessionId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isTooManyRequests());
        }

        @Test
        @DisplayName("генерация после окончания cooldown → 202")
        void afterCooldown_returns202() throws Exception {
            String token = registerAndGetToken("summary@test.com");
            String clientId = createClient(token);
            String sessionId = createSession(token, clientId);

            // Саммари > 60 мин назад (cooldown истёк)
            Session session = sessionRepository.findById(UUID.fromString(sessionId)).orElseThrow();
            session.setAiSummary("Старое саммари");
            session.setAiSummaryGeneratedAt(OffsetDateTime.now().minusMinutes(90));
            sessionRepository.save(session);

            mockMvc.perform(post("/api/sessions/{id}/summary/generate", sessionId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isAccepted());
        }

        @Test
        @DisplayName("без токена → 401")
        void noToken_returns401() throws Exception {
            mockMvc.perform(post("/api/sessions/{id}/summary/generate", UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("чужая сессия → 404")
        void otherUserSession_returns404() throws Exception {
            String tokenA = registerAndGetToken("summaryA@test.com");
            String tokenB = registerAndGetToken("summaryB@test.com");
            String clientId = createClient(tokenA);
            String sessionId = createSession(tokenA, clientId);

            mockMvc.perform(post("/api/sessions/{id}/summary/generate", sessionId)
                            .header("Authorization", "Bearer " + tokenB))
                    .andExpect(status().isNotFound());
        }
    }

    // ── POST /api/clients/{id}/summary/generate ───────────────────────────────

    @Nested
    @DisplayName("POST /api/clients/{id}/summary/generate")
    class TriggerClientSummary {

        @Test
        @DisplayName("< 2 сессий с ai_summary → 400")
        void insufficientSessions_returns400() throws Exception {
            String token = registerAndGetToken("summary@test.com");
            String clientId = createClient(token);

            // Клиент без сессий с саммари
            mockMvc.perform(post("/api/clients/{id}/summary/generate", clientId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Недостаточно сессий для саммари"));
        }

        @Test
        @DisplayName("0 сессий → 400")
        void zeroSessions_returns400() throws Exception {
            String token = registerAndGetToken("summary@test.com");
            String clientId = createClient(token);

            mockMvc.perform(post("/api/clients/{id}/summary/generate", clientId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("2+ сессий с ai_summary → 202 Accepted")
        void enoughSessions_returns202() throws Exception {
            String token = registerAndGetToken("summary@test.com");
            String clientId = createClient(token);

            // Создаём 2 сессии с ai_summary
            String s1 = createSession(token, clientId);
            String s2 = createSession(token, clientId);
            setSessionSummary(s1);
            setSessionSummary(s2);

            mockMvc.perform(post("/api/clients/{id}/summary/generate", clientId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isAccepted());
        }

        @Test
        @DisplayName("cooldown активен → 429")
        void cooldownActive_returns429() throws Exception {
            String token = registerAndGetToken("summary@test.com");
            String clientId = createClient(token);

            // Ставим ai_overall_summary_generated_at в пределах cooldown
            Client client = clientRepository.findById(UUID.fromString(clientId)).orElseThrow();
            client.setAiOverallSummaryGeneratedAt(OffsetDateTime.now().minusMinutes(10));
            clientRepository.save(client);

            mockMvc.perform(post("/api/clients/{id}/summary/generate", clientId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isTooManyRequests());
        }

        @Test
        @DisplayName("без токена → 401")
        void noToken_returns401() throws Exception {
            mockMvc.perform(post("/api/clients/{id}/summary/generate", UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("чужой клиент → 404")
        void otherUserClient_returns404() throws Exception {
            String tokenA = registerAndGetToken("summaryA@test.com");
            String tokenB = registerAndGetToken("summaryB@test.com");
            String clientId = createClient(tokenA);

            mockMvc.perform(post("/api/clients/{id}/summary/generate", clientId)
                            .header("Authorization", "Bearer " + tokenB))
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
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    private String createClient(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Тест\",\"lastName\":\"Клиент\"}")
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }

    private String createSession(String token, String clientId) throws Exception {
        CreateSessionRequest req = new CreateSessionRequest();
        req.setClientId(UUID.fromString(clientId));
        req.setScheduledAt(OffsetDateTime.of(2025, 6, 15, 10, 0, 0, 0, ZoneOffset.UTC));
        req.setDurationMin(90);
        req.setPrice(new BigDecimal("3000.00"));

        MvcResult result = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }

    private void setSessionSummary(String sessionId) {
        Session session = sessionRepository.findById(UUID.fromString(sessionId)).orElseThrow();
        session.setStatus(Session.Status.COMPLETED);
        session.setAiSummary("Тестовое саммари сессии для клиентского суммари");
        session.setAiSummaryGeneratedAt(OffsetDateTime.now().minusHours(3));
        sessionRepository.save(session);
    }
}
