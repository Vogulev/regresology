package com.vogulev.regreso.controller;

import tools.jackson.databind.json.JsonMapper;
import com.vogulev.regreso.BaseIntegrationTest;
import com.vogulev.regreso.dto.request.CreateSessionRequest;
import com.vogulev.regreso.dto.request.RegisterRequest;
import com.vogulev.regreso.dto.request.UpdateSessionRequest;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("ScheduleController — интеграционные тесты")
class ScheduleControllerIntegrationTest extends BaseIntegrationTest {

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

    // ── GET /api/schedule/today ───────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/schedule/today")
    class GetToday {

        @Test
        @DisplayName("без токена → 401")
        void noToken_shouldReturn401() throws Exception {
            mockMvc.perform(get("/api/schedule/today"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("нет сессий сегодня → пустой список, все счётчики 0")
        void noSessions_shouldReturnEmpty() throws Exception {
            String token = registerAndGetToken("user@test.com");

            mockMvc.perform(get("/api/schedule/today")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessions").isArray())
                    .andExpect(jsonPath("$.sessions", hasSize(0)))
                    .andExpect(jsonPath("$.totalCount").value(0))
                    .andExpect(jsonPath("$.completedCount").value(0))
                    .andExpect(jsonPath("$.upcomingCount").value(0));
        }

        @Test
        @DisplayName("сессия сегодня → попадает в ответ с корректными полями")
        void todaySession_shouldAppear() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);
            createSession(token, clientId, OffsetDateTime.now(ZoneOffset.UTC));

            mockMvc.perform(get("/api/schedule/today")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessions", hasSize(1)))
                    .andExpect(jsonPath("$.totalCount").value(1))
                    .andExpect(jsonPath("$.upcomingCount").value(1))
                    .andExpect(jsonPath("$.completedCount").value(0))
                    .andExpect(jsonPath("$.sessions[0].clientFullName").isNotEmpty())
                    .andExpect(jsonPath("$.sessions[0].status").value("SCHEDULED"))
                    .andExpect(jsonPath("$.sessions[0].sessionNumber").value(1))
                    .andExpect(jsonPath("$.sessions[0].telegramReminderSent").value(false));
        }

        @Test
        @DisplayName("сессия вчера → не попадает в today")
        void yesterdaySession_shouldNotAppear() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);
            createSession(token, clientId, OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));

            mockMvc.perform(get("/api/schedule/today")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessions", hasSize(0)))
                    .andExpect(jsonPath("$.totalCount").value(0));
        }

        @Test
        @DisplayName("сессия через 2 дня → не попадает в today")
        void futureDaySession_shouldNotAppearInToday() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);
            createSession(token, clientId, OffsetDateTime.now(ZoneOffset.UTC).plusDays(2));

            mockMvc.perform(get("/api/schedule/today")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessions", hasSize(0)));
        }

        @Test
        @DisplayName("чужой практик не видит сессии другого")
        void otherPractitioner_shouldNotSeeSessions() throws Exception {
            String tokenA = registerAndGetToken("userA@test.com");
            String tokenB = registerAndGetToken("userB@test.com");
            String clientId = createClient(tokenA);
            createSession(tokenA, clientId, OffsetDateTime.now(ZoneOffset.UTC));

            mockMvc.perform(get("/api/schedule/today")
                            .header("Authorization", "Bearer " + tokenB))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessions", hasSize(0)));
        }

        @Test
        @DisplayName("завершённая сессия → completedCount растёт, upcomingCount не растёт")
        void completedSession_countsCorrectly() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);
            String sessionId = createSession(token, clientId, OffsetDateTime.now(ZoneOffset.UTC));
            completeSession(token, sessionId);

            mockMvc.perform(get("/api/schedule/today")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCount").value(1))
                    .andExpect(jsonPath("$.completedCount").value(1))
                    .andExpect(jsonPath("$.upcomingCount").value(0))
                    .andExpect(jsonPath("$.sessions[0].status").value("COMPLETED"));
        }

        @Test
        @DisplayName("несколько сессий сегодня → все в ответе, счётчики корректны")
        void multipleSessions_allCounted() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);

            String s1 = createSession(token, clientId, todayAtPractitionerHour(9));
            String s2 = createSession(token, clientId, todayAtPractitionerHour(14));
            completeSession(token, s1);

            mockMvc.perform(get("/api/schedule/today")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessions", hasSize(2)))
                    .andExpect(jsonPath("$.totalCount").value(2))
                    .andExpect(jsonPath("$.completedCount").value(1))
                    .andExpect(jsonPath("$.upcomingCount").value(1));
        }

        @Test
        @DisplayName("isPaid корректно сериализуется")
        void isPaid_correctlySerialized() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);
            String sessionId = createSession(token, clientId, OffsetDateTime.now(ZoneOffset.UTC));

            UpdateSessionRequest upd = new UpdateSessionRequest();
            upd.setIsPaid(true);
            mockMvc.perform(put("/api/sessions/{id}", sessionId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(upd))
                    .header("Authorization", "Bearer " + token));

            mockMvc.perform(get("/api/schedule/today")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessions[0].isPaid").value(true));
        }
    }

    // ── GET /api/schedule/week ────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/schedule/week")
    class GetWeek {

        @Test
        @DisplayName("без токена → 401")
        void noToken_shouldReturn401() throws Exception {
            mockMvc.perform(get("/api/schedule/week"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("нет сессий → пустой список")
        void noSessions_shouldReturnEmpty() throws Exception {
            String token = registerAndGetToken("user@test.com");

            mockMvc.perform(get("/api/schedule/week")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessions", hasSize(0)))
                    .andExpect(jsonPath("$.totalCount").value(0));
        }

        @Test
        @DisplayName("сессия сегодня → попадает в week")
        void todaySession_inWeek() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);
            createSession(token, clientId, OffsetDateTime.now(ZoneOffset.UTC));

            mockMvc.perform(get("/api/schedule/week")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessions", hasSize(1)));
        }

        @Test
        @DisplayName("сессия через 5 дней → попадает в week")
        void in5Days_inWeek() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);
            createSession(token, clientId, OffsetDateTime.now(ZoneOffset.UTC).plusDays(5));

            mockMvc.perform(get("/api/schedule/week")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessions", hasSize(1)));
        }

        @Test
        @DisplayName("сессия через 8 дней → не попадает в week")
        void in8Days_notInWeek() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);
            createSession(token, clientId, OffsetDateTime.now(ZoneOffset.UTC).plusDays(8));

            mockMvc.perform(get("/api/schedule/week")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessions", hasSize(0)));
        }

        @Test
        @DisplayName("сессия вчера → не попадает в week")
        void yesterdaySession_notInWeek() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);
            createSession(token, clientId, OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));

            mockMvc.perform(get("/api/schedule/week")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessions", hasSize(0)));
        }

        @Test
        @DisplayName("сессии отсортированы по времени")
        void sessions_sortedByScheduledAt() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);

            // Создаём в обратном порядке
            createSession(token, clientId, OffsetDateTime.now(ZoneOffset.UTC).plusDays(3));
            createSession(token, clientId, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
            createSession(token, clientId, OffsetDateTime.now(ZoneOffset.UTC));

            MvcResult result = mockMvc.perform(get("/api/schedule/week")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessions", hasSize(3)))
                    .andReturn();

            // Проверяем sessionNumber как прокси порядка (1 создан позже, но scheduledAt раньше)
            // Ключевое: первый элемент должен быть ближайшим по времени
            var tree = objectMapper.readTree(result.getResponse().getContentAsString());
            var s = tree.get("sessions");
            org.assertj.core.api.Assertions.assertThat(
                    s.get(0).get("scheduledAt").asText()
            ).isLessThanOrEqualTo(
                    s.get(1).get("scheduledAt").asText()
            );
        }

        @Test
        @DisplayName("чужой практик не видит сессии другого")
        void otherPractitioner_cannotSeeSessions() throws Exception {
            String tokenA = registerAndGetToken("userA@test.com");
            String tokenB = registerAndGetToken("userB@test.com");
            String clientId = createClient(tokenA);
            createSession(tokenA, clientId, OffsetDateTime.now(ZoneOffset.UTC).plusDays(2));

            mockMvc.perform(get("/api/schedule/week")
                            .header("Authorization", "Bearer " + tokenB))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessions", hasSize(0)));
        }

        @Test
        @DisplayName("смешанные статусы → счётчики правильные")
        void mixedStatuses_countsCorrect() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);

            String s1 = createSession(token, clientId, OffsetDateTime.now(ZoneOffset.UTC));
            String s2 = createSession(token, clientId, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
            String s3 = createSession(token, clientId, OffsetDateTime.now(ZoneOffset.UTC).plusDays(2));
            completeSession(token, s1);
            cancelSession(token, s2);

            mockMvc.perform(get("/api/schedule/week")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCount").value(3))
                    .andExpect(jsonPath("$.completedCount").value(1))
                    .andExpect(jsonPath("$.upcomingCount").value(1)); // только SCHEDULED
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
                        .content("{\"firstName\":\"Тестовый\",\"lastName\":\"Клиент\"}")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }

    private String createSession(String token, String clientId, OffsetDateTime scheduledAt) throws Exception {
        CreateSessionRequest req = new CreateSessionRequest();
        req.setClientId(UUID.fromString(clientId));
        req.setScheduledAt(scheduledAt);
        req.setDurationMin(90);
        req.setPrice(new BigDecimal("3000.00"));

        MvcResult result = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }

    private void completeSession(String token, String sessionId) throws Exception {
        mockMvc.perform(post("/api/sessions/{id}/complete", sessionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private void cancelSession(String token, String sessionId) throws Exception {
        mockMvc.perform(post("/api/sessions/{id}/cancel", sessionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private OffsetDateTime todayAtPractitionerHour(int hour) {
        ZoneId practitionerZone = ZoneId.of("Europe/Moscow");
        return LocalDate.now(practitionerZone)
                .atTime(hour, 0)
                .atZone(practitionerZone)
                .toOffsetDateTime();
    }
}
