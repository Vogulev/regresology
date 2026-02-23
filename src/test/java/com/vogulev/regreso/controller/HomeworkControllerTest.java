package com.vogulev.regreso.controller;

import tools.jackson.databind.json.JsonMapper;
import com.vogulev.regreso.BaseIntegrationTest;
import com.vogulev.regreso.dto.request.CreateHomeworkRequest;
import com.vogulev.regreso.dto.request.CreateSessionRequest;
import com.vogulev.regreso.dto.request.MaterialRequest;
import com.vogulev.regreso.dto.request.RegisterRequest;
import com.vogulev.regreso.dto.request.UpdateHomeworkStatusRequest;
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

@DisplayName("HomeworkController — интеграционные тесты")
class HomeworkControllerTest extends BaseIntegrationTest {

    @Autowired JsonMapper objectMapper;
    @Autowired HomeworkRepository homeworkRepository;
    @Autowired ReminderRepository reminderRepository;
    @Autowired SessionRepository sessionRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired MaterialRepository materialRepository;
    @Autowired PractitionerRepository practitionerRepository;

    @BeforeEach
    void cleanUp() {
        reminderRepository.deleteAll();
        homeworkRepository.deleteAll();
        sessionRepository.deleteAll();
        clientRepository.deleteAll();
        materialRepository.deleteAll();
        practitionerRepository.deleteAll();
    }

    // ── GET /api/clients/{clientId}/homework ──────────────────────────────────

    @Nested
    @DisplayName("GET /api/clients/{clientId}/homework")
    class GetClientHomework {

        @Test
        @DisplayName("без токена → 401")
        void noToken_returns401() throws Exception {
            mockMvc.perform(get("/api/clients/{id}/homework", UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("пустой список → 200 []")
        void empty_returns200() throws Exception {
            String token = registerAndGetToken("hw@test.com");
            String clientId = createClient(token);

            mockMvc.perform(get("/api/clients/{clientId}/homework", clientId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("чужой клиент → 404")
        void otherClient_returns404() throws Exception {
            String tokenA = registerAndGetToken("hwA@test.com");
            String tokenB = registerAndGetToken("hwB@test.com");
            String clientId = createClient(tokenA);

            mockMvc.perform(get("/api/clients/{clientId}/homework", clientId)
                            .header("Authorization", "Bearer " + tokenB))
                    .andExpect(status().isNotFound());
        }
    }

    // ── POST /api/clients/{clientId}/homework ─────────────────────────────────

    @Nested
    @DisplayName("POST /api/clients/{clientId}/homework")
    class CreateForClient {

        @Test
        @DisplayName("с title/description → 201")
        void withTitleDescription_returns201() throws Exception {
            String token = registerAndGetToken("hw@test.com");
            String clientId = createClient(token);

            CreateHomeworkRequest req = new CreateHomeworkRequest();
            req.setTitle("Дневник сновидений");
            req.setDescription("Записывать сны каждое утро");
            req.setHomeworkType("JOURNALING");

            mockMvc.perform(post("/api/clients/{clientId}/homework", clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.title").value("Дневник сновидений"))
                    .andExpect(jsonPath("$.status").value("ASSIGNED"))
                    .andExpect(jsonPath("$.clientId").value(clientId))
                    .andExpect(jsonPath("$.sessionId").doesNotExist());
        }

        @Test
        @DisplayName("с materialId → title/description берутся из материала")
        void withMaterialId_titleFromMaterial() throws Exception {
            String token = registerAndGetToken("hw@test.com");
            String clientId = createClient(token);
            String materialId = createMaterial(token, "Медитация осознанности", "meditation",
                    "Практика ежедневной медитации");

            CreateHomeworkRequest req = new CreateHomeworkRequest();
            req.setMaterialId(UUID.fromString(materialId));

            mockMvc.perform(post("/api/clients/{clientId}/homework", clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("Медитация осознанности"))
                    .andExpect(jsonPath("$.description").value("Практика ежедневной медитации"))
                    .andExpect(jsonPath("$.materialId").value(materialId));
        }

        @Test
        @DisplayName("materialId + свой title → title переопределяет материал")
        void materialIdWithCustomTitle_titleOverrides() throws Exception {
            String token = registerAndGetToken("hw@test.com");
            String clientId = createClient(token);
            String materialId = createMaterial(token, "Материальный заголовок", "book",
                    "Описание книги");

            CreateHomeworkRequest req = new CreateHomeworkRequest();
            req.setMaterialId(UUID.fromString(materialId));
            req.setTitle("Мой заголовок");
            req.setDescription("Моё описание");

            mockMvc.perform(post("/api/clients/{clientId}/homework", clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("Мой заголовок"))
                    .andExpect(jsonPath("$.description").value("Моё описание"));
        }

        @Test
        @DisplayName("materialId из другого практика → 404")
        void otherPractitionerMaterial_returns404() throws Exception {
            String tokenA = registerAndGetToken("hwA@test.com");
            String tokenB = registerAndGetToken("hwB@test.com");
            String clientId = createClient(tokenA);
            String materialId = createMaterial(tokenB, "Чужой материал", "book", "Описание");

            CreateHomeworkRequest req = new CreateHomeworkRequest();
            req.setMaterialId(UUID.fromString(materialId));

            mockMvc.perform(post("/api/clients/{clientId}/homework", clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("без title и без materialId → 400")
        void noTitleNoMaterial_returns400() throws Exception {
            String token = registerAndGetToken("hw@test.com");
            String clientId = createClient(token);

            CreateHomeworkRequest req = new CreateHomeworkRequest();
            req.setDescription("Описание есть, заголовка нет");

            mockMvc.perform(post("/api/clients/{clientId}/homework", clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("несуществующий materialId → 404")
        void unknownMaterialId_returns404() throws Exception {
            String token = registerAndGetToken("hw@test.com");
            String clientId = createClient(token);

            CreateHomeworkRequest req = new CreateHomeworkRequest();
            req.setMaterialId(UUID.randomUUID());

            mockMvc.perform(post("/api/clients/{clientId}/homework", clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }
    }

    // ── POST /api/sessions/{sessionId}/homework ───────────────────────────────

    @Nested
    @DisplayName("POST /api/sessions/{sessionId}/homework")
    class CreateForSession {

        @Test
        @DisplayName("привязка к сессии → 201, sessionId заполнен")
        void linkedToSession_returns201() throws Exception {
            String token = registerAndGetToken("hw@test.com");
            String clientId = createClient(token);
            String sessionId = createSession(token, clientId);

            CreateHomeworkRequest req = new CreateHomeworkRequest();
            req.setTitle("Задание после сессии");
            req.setDescription("Что делать до следующей сессии");

            mockMvc.perform(post("/api/sessions/{sessionId}/homework", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.sessionId").value(sessionId))
                    .andExpect(jsonPath("$.clientId").value(clientId))
                    .andExpect(jsonPath("$.title").value("Задание после сессии"));
        }

        @Test
        @DisplayName("чужая сессия → 404")
        void otherUserSession_returns404() throws Exception {
            String tokenA = registerAndGetToken("hwA@test.com");
            String tokenB = registerAndGetToken("hwB@test.com");
            String clientId = createClient(tokenA);
            String sessionId = createSession(tokenA, clientId);

            CreateHomeworkRequest req = new CreateHomeworkRequest();
            req.setTitle("Взлом");
            req.setDescription("Описание");

            mockMvc.perform(post("/api/sessions/{sessionId}/homework", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + tokenB))
                    .andExpect(status().isNotFound());
        }
    }

    // ── PATCH /api/homework/{id}/status ──────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/homework/{id}/status")
    class UpdateStatus {

        @Test
        @DisplayName("COMPLETED → 200")
        void completed_returns200() throws Exception {
            String token = registerAndGetToken("hw@test.com");
            String clientId = createClient(token);
            String hwId = createHomework(token, clientId);

            UpdateHomeworkStatusRequest req = new UpdateHomeworkStatusRequest();
            req.setStatus("COMPLETED");

            mockMvc.perform(patch("/api/homework/{id}/status", hwId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("SKIPPED → 200")
        void skipped_returns200() throws Exception {
            String token = registerAndGetToken("hw@test.com");
            String clientId = createClient(token);
            String hwId = createHomework(token, clientId);

            UpdateHomeworkStatusRequest req = new UpdateHomeworkStatusRequest();
            req.setStatus("SKIPPED");

            mockMvc.perform(patch("/api/homework/{id}/status", hwId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SKIPPED"));
        }

        @Test
        @DisplayName("недопустимый статус → 400")
        void invalidStatus_returns400() throws Exception {
            String token = registerAndGetToken("hw@test.com");
            String clientId = createClient(token);
            String hwId = createHomework(token, clientId);

            UpdateHomeworkStatusRequest req = new UpdateHomeworkStatusRequest();
            req.setStatus("ASSIGNED");

            mockMvc.perform(patch("/api/homework/{id}/status", hwId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("чужое задание → 404")
        void otherUserHomework_returns404() throws Exception {
            String tokenA = registerAndGetToken("hwA@test.com");
            String tokenB = registerAndGetToken("hwB@test.com");
            String clientId = createClient(tokenA);
            String hwId = createHomework(tokenA, clientId);

            UpdateHomeworkStatusRequest req = new UpdateHomeworkStatusRequest();
            req.setStatus("COMPLETED");

            mockMvc.perform(patch("/api/homework/{id}/status", hwId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
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

    private String createMaterial(String token, String title, String type, String description) throws Exception {
        MaterialRequest req = new MaterialRequest();
        req.setTitle(title);
        req.setMaterialType(type);
        req.setDescription(description);

        MvcResult result = mockMvc.perform(post("/api/materials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req))
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createHomework(String token, String clientId) throws Exception {
        CreateHomeworkRequest req = new CreateHomeworkRequest();
        req.setTitle("Тестовое задание");
        req.setDescription("Описание задания");

        MvcResult result = mockMvc.perform(post("/api/clients/{clientId}/homework", clientId)
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
