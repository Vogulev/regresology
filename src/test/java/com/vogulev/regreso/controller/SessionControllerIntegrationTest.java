package com.vogulev.regreso.controller;

import tools.jackson.databind.json.JsonMapper;
import com.vogulev.regreso.BaseIntegrationTest;
import com.vogulev.regreso.dto.request.CancelSessionRequest;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("SessionController — интеграционные тесты")
class SessionControllerIntegrationTest extends BaseIntegrationTest {

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

    // ── POST /api/sessions ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/sessions")
    class CreateSession {

        @Test
        @DisplayName("без токена → 401")
        void noToken_shouldReturn401() throws Exception {
            mockMvc.perform(post("/api/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("валидные данные → 201 с сессией")
        void validRequest_shouldReturn201() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);

            CreateSessionRequest req = validSessionRequest(UUID.fromString(clientId));

            mockMvc.perform(post("/api/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.sessionNumber").value(1))
                    .andExpect(jsonPath("$.status").value("SCHEDULED"))
                    .andExpect(jsonPath("$.clientId").value(clientId))
                    .andExpect(jsonPath("$.durationMin").value(90))
                    .andExpect(jsonPath("$.price").value(3000));
        }

        @Test
        @DisplayName("session_number автоинкремент")
        void sessionNumber_autoIncrement() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);

            createSession(token, clientId);
            String sessionId2 = createSession(token, clientId);

            mockMvc.perform(get("/api/sessions/{id}", sessionId2)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionNumber").value(2));
        }

        @Test
        @DisplayName("создание сессии → создаются 2 напоминания")
        void createSession_creates2Reminders() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);

            createSession(token, clientId);

            long count = reminderRepository.count();
            org.assertj.core.api.Assertions.assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("включены напоминания практику → создаются ещё 2 reminder для PRACTITIONER")
        void createSession_withPractitionerRemindersEnabled_createsPractitionerReminders() throws Exception {
            String token = registerAndGetToken("practitioner-reminders@test.com");
            String clientId = createClient(token);

            mockMvc.perform(put("/api/settings/notifications")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "practitionerSessionRemindersEnabled": true
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.practitionerSessionRemindersEnabled").value(true));

            createSession(token, clientId);

            List<com.vogulev.regreso.entity.Reminder> reminders = reminderRepository.findAll();
            org.assertj.core.api.Assertions.assertThat(reminders).hasSize(4);
            org.assertj.core.api.Assertions.assertThat(reminders)
                    .extracting(com.vogulev.regreso.entity.Reminder::getRecipientType)
                    .containsExactlyInAnyOrder(
                            com.vogulev.regreso.entity.Reminder.RecipientType.CLIENT,
                            com.vogulev.regreso.entity.Reminder.RecipientType.CLIENT,
                            com.vogulev.regreso.entity.Reminder.RecipientType.PRACTITIONER,
                            com.vogulev.regreso.entity.Reminder.RecipientType.PRACTITIONER
                    );
        }

        @Test
        @DisplayName("без clientId → 400")
        void missingClientId_shouldReturn400() throws Exception {
            String token = registerAndGetToken("user@test.com");

            mockMvc.perform(post("/api/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"scheduledAt\":\"2025-06-01T10:00:00Z\"}")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("без scheduledAt → 400")
        void missingScheduledAt_shouldReturn400() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);

            mockMvc.perform(post("/api/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"clientId\":\"" + clientId + "\"}")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("чужой клиент → 404")
        void otherUsersClient_shouldReturn404() throws Exception {
            String tokenA = registerAndGetToken("userA@test.com");
            String tokenB = registerAndGetToken("userB@test.com");
            String clientId = createClient(tokenA);

            CreateSessionRequest req = validSessionRequest(UUID.fromString(clientId));

            mockMvc.perform(post("/api/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + tokenB))
                    .andExpect(status().isNotFound());
        }
    }

    // ── GET /api/sessions/{id} ─────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/sessions/{id}")
    class GetSession {

        @Test
        @DisplayName("без токена → 401")
        void noToken_shouldReturn401() throws Exception {
            mockMvc.perform(get("/api/sessions/{id}", UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("существующая сессия → 200 с полным протоколом")
        void existingSession_shouldReturn200() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);
            String sessionId = createSession(token, clientId);

            mockMvc.perform(get("/api/sessions/{id}", sessionId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(sessionId))
                    .andExpect(jsonPath("$.status").value("SCHEDULED"))
                    .andExpect(jsonPath("$.clientId").value(clientId))
                    .andExpect(jsonPath("$.clientFullName").isNotEmpty())
                    .andExpect(jsonPath("$.media").isArray())
                    .andExpect(jsonPath("$.media", hasSize(0)));
        }

        @Test
        @DisplayName("несуществующий ID → 404")
        void randomId_shouldReturn404() throws Exception {
            String token = registerAndGetToken("user@test.com");

            mockMvc.perform(get("/api/sessions/{id}", UUID.randomUUID())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("чужая сессия → 404")
        void otherUsersSession_shouldReturn404() throws Exception {
            String tokenA = registerAndGetToken("userA@test.com");
            String tokenB = registerAndGetToken("userB@test.com");
            String clientId = createClient(tokenA);
            String sessionId = createSession(tokenA, clientId);

            mockMvc.perform(get("/api/sessions/{id}", sessionId)
                            .header("Authorization", "Bearer " + tokenB))
                    .andExpect(status().isNotFound());
        }
    }

    // ── POST /api/sessions/{id}/photos ────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/sessions/{id}/photos")
    class UploadSessionPhoto {

        @Test
        @DisplayName("загрузка фото протокола → 201 и фото видно в GET сессии")
        void uploadPhoto_shouldReturn201() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);
            String sessionId = createSession(token, clientId);

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "notes.jpg",
                    MediaType.IMAGE_JPEG_VALUE,
                    "fake-image-content".getBytes()
            );

            mockMvc.perform(multipart("/api/sessions/{id}/photos", sessionId)
                            .file(file)
                            .param("caption", "Разворот тетради")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.mediaType").value("PHOTO"))
                    .andExpect(jsonPath("$.fileName").value("notes.jpg"))
                    .andExpect(jsonPath("$.mimeType").value(MediaType.IMAGE_JPEG_VALUE))
                    .andExpect(jsonPath("$.caption").value("Разворот тетради"))
                    .andExpect(jsonPath("$.fileUrl", startsWith("/api/files/session-photos/")));

            mockMvc.perform(get("/api/sessions/{id}", sessionId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.media", hasSize(1)))
                    .andExpect(jsonPath("$.media[0].mediaType").value("PHOTO"))
                    .andExpect(jsonPath("$.media[0].caption").value("Разворот тетради"));
        }

        @Test
        @DisplayName("неизображение → 400")
        void nonImage_shouldReturn400() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);
            String sessionId = createSession(token, clientId);

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "notes.pdf",
                    MediaType.APPLICATION_PDF_VALUE,
                    "fake-pdf-content".getBytes()
            );

            mockMvc.perform(multipart("/api/sessions/{id}/photos", sessionId)
                            .file(file)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("чужая сессия → 404")
        void otherUsersSession_shouldReturn404() throws Exception {
            String tokenA = registerAndGetToken("userA@test.com");
            String tokenB = registerAndGetToken("userB@test.com");
            String clientId = createClient(tokenA);
            String sessionId = createSession(tokenA, clientId);

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "notes.jpg",
                    MediaType.IMAGE_JPEG_VALUE,
                    "fake-image-content".getBytes()
            );

            mockMvc.perform(multipart("/api/sessions/{id}/photos", sessionId)
                            .file(file)
                            .header("Authorization", "Bearer " + tokenB))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("удаление фото протокола → 204 и фото исчезает из GET сессии")
        void deletePhoto_shouldReturn204() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);
            String sessionId = createSession(token, clientId);

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "notes.jpg",
                    MediaType.IMAGE_JPEG_VALUE,
                    "fake-image-content".getBytes()
            );

            MvcResult uploadResult = mockMvc.perform(multipart("/api/sessions/{id}/photos", sessionId)
                            .file(file)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isCreated())
                    .andReturn();

            String mediaId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                    .get("id").asText();

            mockMvc.perform(delete("/api/sessions/{id}/photos/{mediaId}", sessionId, mediaId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/sessions/{id}", sessionId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.media", hasSize(0)));
        }

        @Test
        @DisplayName("удаление чужого фото → 404")
        void deleteOtherUsersPhoto_shouldReturn404() throws Exception {
            String tokenA = registerAndGetToken("userA@test.com");
            String tokenB = registerAndGetToken("userB@test.com");
            String clientId = createClient(tokenA);
            String sessionId = createSession(tokenA, clientId);

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "notes.jpg",
                    MediaType.IMAGE_JPEG_VALUE,
                    "fake-image-content".getBytes()
            );

            MvcResult uploadResult = mockMvc.perform(multipart("/api/sessions/{id}/photos", sessionId)
                            .file(file)
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isCreated())
                    .andReturn();

            String mediaId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                    .get("id").asText();

            mockMvc.perform(delete("/api/sessions/{id}/photos/{mediaId}", sessionId, mediaId)
                            .header("Authorization", "Bearer " + tokenB))
                    .andExpect(status().isNotFound());
        }
    }

    // ── PUT /api/sessions/{id} ─────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /api/sessions/{id}")
    class UpdateSession {

        @Test
        @DisplayName("обновление полей протокола → 200")
        void updateProtocol_shouldReturn200() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);
            String sessionId = createSession(token, clientId);

            UpdateSessionRequest req = new UpdateSessionRequest();
            req.setPreSessionRequest("Страхи одиночества");
            req.setPreSessionState("Тревожный");
            req.setPreSessionScore((short) 4);
            req.setRegressionTarget("PAST_LIFE");
            req.setKeyInsights("Инсайт о прошлой жизни");
            req.setPractitionerNotes("Приватные заметки");

            mockMvc.perform(put("/api/sessions/{id}", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.preSessionRequest").value("Страхи одиночества"))
                    .andExpect(jsonPath("$.preSessionState").value("Тревожный"))
                    .andExpect(jsonPath("$.preSessionScore").value(4))
                    .andExpect(jsonPath("$.regressionTarget").value("PAST_LIFE"))
                    .andExpect(jsonPath("$.keyInsights").value("Инсайт о прошлой жизни"))
                    .andExpect(jsonPath("$.practitionerNotes").value("Приватные заметки"));
        }

        @Test
        @DisplayName("обновить tranceDepth → 200")
        void updateTranceDepth_shouldReturn200() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);
            String sessionId = createSession(token, clientId);

            UpdateSessionRequest req = new UpdateSessionRequest();
            req.setTranceDepth("DEEP");
            req.setInductionMethod("Лестница");

            mockMvc.perform(put("/api/sessions/{id}", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tranceDepth").value("DEEP"))
                    .andExpect(jsonPath("$.inductionMethod").value("Лестница"));
        }

        @Test
        @DisplayName("некорректный tranceDepth → 500 (IllegalArgumentException)")
        void invalidTranceDepth_shouldFail() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);
            String sessionId = createSession(token, clientId);

            UpdateSessionRequest req = new UpdateSessionRequest();
            req.setTranceDepth("INVALID_VALUE");

            mockMvc.perform(put("/api/sessions/{id}", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("чужая сессия → 404")
        void otherUsersSession_shouldReturn404() throws Exception {
            String tokenA = registerAndGetToken("userA@test.com");
            String tokenB = registerAndGetToken("userB@test.com");
            String clientId = createClient(tokenA);
            String sessionId = createSession(tokenA, clientId);

            UpdateSessionRequest req = new UpdateSessionRequest();
            req.setKeyInsights("Попытка записи");

            mockMvc.perform(put("/api/sessions/{id}", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + tokenB))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("финансовые поля обновляются")
        void updateFinancials_shouldReturn200() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);
            String sessionId = createSession(token, clientId);

            UpdateSessionRequest req = new UpdateSessionRequest();
            req.setPrice(new BigDecimal("5000.00"));
            req.setIsPaid(true);

            mockMvc.perform(put("/api/sessions/{id}", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.price").value(5000.00))
                    .andExpect(jsonPath("$.isPaid").value(true));
        }
    }

    // ── POST /api/sessions/{id}/complete ──────────────────────────────────────

    @Nested
    @DisplayName("POST /api/sessions/{id}/complete")
    class CompleteSession {

        @Test
        @DisplayName("завершить сессию → 200 COMPLETED")
        void complete_shouldReturn200() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);
            String sessionId = createSession(token, clientId);

            mockMvc.perform(post("/api/sessions/{id}/complete", sessionId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("завершить с финальными полями → 200")
        void completeWithFields_shouldReturn200() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);
            String sessionId = createSession(token, clientId);

            UpdateSessionRequest req = new UpdateSessionRequest();
            req.setPostSessionState("Спокойный");
            req.setPostSessionScore((short) 8);
            req.setNextSessionPlan("Проработать детство");

            mockMvc.perform(post("/api/sessions/{id}/complete", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.postSessionState").value("Спокойный"))
                    .andExpect(jsonPath("$.postSessionScore").value(8))
                    .andExpect(jsonPath("$.nextSessionPlan").value("Проработать детство"));
        }

        @Test
        @DisplayName("повторное завершение → 400")
        void doubleComplete_shouldReturn400() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);
            String sessionId = createSession(token, clientId);

            mockMvc.perform(post("/api/sessions/{id}/complete", sessionId)
                    .header("Authorization", "Bearer " + token));

            mockMvc.perform(post("/api/sessions/{id}/complete", sessionId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("завершить отменённую → 400")
        void completeCancelled_shouldReturn400() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);
            String sessionId = createSession(token, clientId);

            mockMvc.perform(post("/api/sessions/{id}/cancel", sessionId)
                    .header("Authorization", "Bearer " + token));

            mockMvc.perform(post("/api/sessions/{id}/complete", sessionId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("чужая сессия → 404")
        void otherUsersSession_shouldReturn404() throws Exception {
            String tokenA = registerAndGetToken("userA@test.com");
            String tokenB = registerAndGetToken("userB@test.com");
            String clientId = createClient(tokenA);
            String sessionId = createSession(tokenA, clientId);

            mockMvc.perform(post("/api/sessions/{id}/complete", sessionId)
                            .header("Authorization", "Bearer " + tokenB))
                    .andExpect(status().isNotFound());
        }
    }

    // ── POST /api/sessions/{id}/cancel ────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/sessions/{id}/cancel")
    class CancelSession {

        @Test
        @DisplayName("отменить сессию → 200 CANCELLED")
        void cancel_shouldReturn200() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);
            String sessionId = createSession(token, clientId);

            mockMvc.perform(post("/api/sessions/{id}/cancel", sessionId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        @DisplayName("отменить с причиной → 200, причина в practitionerNotes")
        void cancelWithReason_shouldReturn200() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);
            String sessionId = createSession(token, clientId);

            CancelSessionRequest req = new CancelSessionRequest();
            req.setReason("Клиент заболел");

            mockMvc.perform(post("/api/sessions/{id}/cancel", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"))
                    .andExpect(jsonPath("$.practitionerNotes").value("Клиент заболел"));
        }

        @Test
        @DisplayName("повторная отмена → 400")
        void doubleCancel_shouldReturn400() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);
            String sessionId = createSession(token, clientId);

            mockMvc.perform(post("/api/sessions/{id}/cancel", sessionId)
                    .header("Authorization", "Bearer " + token));

            mockMvc.perform(post("/api/sessions/{id}/cancel", sessionId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("отменить завершённую → 400")
        void cancelCompleted_shouldReturn400() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);
            String sessionId = createSession(token, clientId);

            mockMvc.perform(post("/api/sessions/{id}/complete", sessionId)
                    .header("Authorization", "Bearer " + token));

            mockMvc.perform(post("/api/sessions/{id}/cancel", sessionId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── GET /api/sessions/{id}/prepare ────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/sessions/{id}/prepare")
    class GetSessionPrep {

        @Test
        @DisplayName("без токена → 401")
        void noToken_shouldReturn401() throws Exception {
            mockMvc.perform(get("/api/sessions/{id}/prepare", UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("экран подготовки → 200 с данными клиента")
        void prepScreen_shouldReturn200() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);
            String sessionId = createSession(token, clientId);

            mockMvc.perform(get("/api/sessions/{id}/prepare", sessionId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value(sessionId))
                    .andExpect(jsonPath("$.clientId").value(clientId))
                    .andExpect(jsonPath("$.clientFullName").isNotEmpty())
                    .andExpect(jsonPath("$.sessionNumber").value(1))
                    .andExpect(jsonPath("$.recentSessions").isArray())
                    .andExpect(jsonPath("$.activeThemes").isArray())
                    .andExpect(jsonPath("$.hasContraindications").value(false));
        }

        @Test
        @DisplayName("план из предыдущей сессии попадает в nextSessionPlan")
        void previousNextSessionPlan_shouldAppear() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token);

            // Первая сессия — ставим план
            String session1Id = createSession(token, clientId);
            UpdateSessionRequest upd = new UpdateSessionRequest();
            upd.setNextSessionPlan("Работать с детством в следующий раз");
            mockMvc.perform(post("/api/sessions/{id}/complete", session1Id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(upd))
                    .header("Authorization", "Bearer " + token));

            // Вторая сессия
            String session2Id = createSession(token, clientId);

            mockMvc.perform(get("/api/sessions/{id}/prepare", session2Id)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nextSessionPlan").value("Работать с детством в следующий раз"));
        }

        @Test
        @DisplayName("несуществующая сессия → 404")
        void unknownSession_shouldReturn404() throws Exception {
            String token = registerAndGetToken("user@test.com");

            mockMvc.perform(get("/api/sessions/{id}/prepare", UUID.randomUUID())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("чужая сессия → 404")
        void otherUsersSession_shouldReturn404() throws Exception {
            String tokenA = registerAndGetToken("userA@test.com");
            String tokenB = registerAndGetToken("userB@test.com");
            String clientId = createClient(tokenA);
            String sessionId = createSession(tokenA, clientId);

            mockMvc.perform(get("/api/sessions/{id}/prepare", sessionId)
                            .header("Authorization", "Bearer " + tokenB))
                    .andExpect(status().isNotFound());
        }
    }

    // ── GET /api/sessions/{id}/summary ────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/sessions/{id}/summary")
    class GetSessionSummary {

        @Test
        @DisplayName("саммари → 200 PENDING (AI не подключён)")
        void summary_shouldReturn200Pending() throws Exception {
            String token = registerAndGetToken("user@test.com");
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
        @DisplayName("чужая сессия → 404")
        void otherUsersSession_shouldReturn404() throws Exception {
            String tokenA = registerAndGetToken("userA@test.com");
            String tokenB = registerAndGetToken("userB@test.com");
            String clientId = createClient(tokenA);
            String sessionId = createSession(tokenA, clientId);

            mockMvc.perform(get("/api/sessions/{id}/summary", sessionId)
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

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    private String createClient(String token) throws Exception {
        String body = "{\"firstName\":\"Тестовый\",\"lastName\":\"Клиент\"}";

        MvcResult result = mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }

    private String createSession(String token, String clientId) throws Exception {
        CreateSessionRequest req = validSessionRequest(UUID.fromString(clientId));

        MvcResult result = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }

    private CreateSessionRequest validSessionRequest(UUID clientId) {
        CreateSessionRequest req = new CreateSessionRequest();
        req.setClientId(clientId);
        req.setScheduledAt(OffsetDateTime.of(2025, 6, 15, 10, 0, 0, 0, ZoneOffset.UTC));
        req.setDurationMin(90);
        req.setPrice(new BigDecimal("3000.00"));
        return req;
    }

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}
