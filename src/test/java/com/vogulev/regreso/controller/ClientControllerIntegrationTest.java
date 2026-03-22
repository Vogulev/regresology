package com.vogulev.regreso.controller;

import tools.jackson.databind.json.JsonMapper;
import com.vogulev.regreso.BaseIntegrationTest;
import com.vogulev.regreso.dto.request.ClientRequest;
import com.vogulev.regreso.dto.request.RegisterRequest;
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

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("ClientController — интеграционные тесты")
class ClientControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired JsonMapper objectMapper;
    @Autowired BookingSettingsRepository bookingSettingsRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired PractitionerRepository practitionerRepository;
    @Autowired SessionRepository sessionRepository;
    @Autowired ReminderRepository reminderRepository;

    @BeforeEach
    void cleanUp() {
        bookingSettingsRepository.deleteAll();
        reminderRepository.deleteAll();
        sessionRepository.deleteAll();
        clientRepository.deleteAll();
        practitionerRepository.deleteAll();
    }

    // ── GET /api/clients ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/clients")
    class GetClients {

        @Test
        @DisplayName("без токена → 401")
        void noToken_shouldReturn401() throws Exception {
            mockMvc.perform(get("/api/clients"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("пустой список → 200 []")
        void emptyList_shouldReturn200() throws Exception {
            String token = registerAndGetToken("user@test.com");

            mockMvc.perform(get("/api/clients")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("один клиент → 200 с записью")
        void oneClient_shouldReturnList() throws Exception {
            String token = registerAndGetToken("user@test.com");
            createClient(token, validClientRequest("Иван", "Иванов"));

            mockMvc.perform(get("/api/clients")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].fullName").value("Иван Иванов"))
                    .andExpect(jsonPath("$[0].totalSessions").value(0))
                    .andExpect(jsonPath("$[0].activeHomeworkCount").value(0));
        }

        @Test
        @DisplayName("другой пользователь не видит чужих клиентов")
        void otherUser_cannotSeeClients() throws Exception {
            String tokenA = registerAndGetToken("userA@test.com");
            String tokenB = registerAndGetToken("userB@test.com");

            createClient(tokenA, validClientRequest("Клиент A", null));

            mockMvc.perform(get("/api/clients")
                            .header("Authorization", "Bearer " + tokenB))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("архивированный клиент не отображается в списке")
        void archivedClient_notInList() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token, validClientRequest("Архивный", null));

            mockMvc.perform(patch("/api/clients/{id}/archive", clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"archive\":true}")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/clients")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ── POST /api/clients ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/clients")
    class CreateClient {

        @Test
        @DisplayName("валидные данные → 201 с полным ответом")
        void validRequest_shouldReturn201() throws Exception {
            String token = registerAndGetToken("user@test.com");
            ClientRequest req = validClientRequest("Мария", "Петрова");
            req.setPresentingIssues(List.of("страхи", "отношения"));
            req.setHasContraindications(true);
            req.setContraindicationsNotes("Эпилепсия");

            mockMvc.perform(post("/api/clients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.firstName").value("Мария"))
                    .andExpect(jsonPath("$.lastName").value("Петрова"))
                    .andExpect(jsonPath("$.fullName").value("Мария Петрова"))
                    .andExpect(jsonPath("$.hasContraindications").value(true))
                    .andExpect(jsonPath("$.contraindicationsNotes").value("Эпилепсия"))
                    .andExpect(jsonPath("$.presentingIssues", hasSize(2)))
                    .andExpect(jsonPath("$.totalSessions").value(0))
                    .andExpect(jsonPath("$.telegramConnected").value(false));
        }

        @Test
        @DisplayName("пустое имя → 400")
        void blankFirstName_shouldReturn400() throws Exception {
            String token = registerAndGetToken("user@test.com");
            ClientRequest req = validClientRequest("", null);

            mockMvc.perform(post("/api/clients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.firstName").exists());
        }

        @Test
        @DisplayName("некорректный телефон → 400")
        void invalidPhone_shouldReturn400() throws Exception {
            String token = registerAndGetToken("user@test.com");
            ClientRequest req = validClientRequest("Иван", null);
            req.setPhone("not-a-phone");

            mockMvc.perform(post("/api/clients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.phone").exists());
        }

        @Test
        @DisplayName("некорректный email → 400")
        void invalidEmail_shouldReturn400() throws Exception {
            String token = registerAndGetToken("user@test.com");
            ClientRequest req = validClientRequest("Иван", null);
            req.setEmail("not-an-email");

            mockMvc.perform(post("/api/clients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.email").exists());
        }

        @Test
        @DisplayName("без токена → 401")
        void noToken_shouldReturn401() throws Exception {
            mockMvc.perform(post("/api/clients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(validClientRequest("Иван", null))))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── GET /api/clients/{id} ──────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/clients/{id}")
    class GetClientById {

        @Test
        @DisplayName("существующий клиент → 200 с полной карточкой")
        void existingClient_shouldReturn200() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token, validClientRequest("Сергей", "Смирнов"));

            mockMvc.perform(get("/api/clients/{id}", clientId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(clientId))
                    .andExpect(jsonPath("$.firstName").value("Сергей"))
                    .andExpect(jsonPath("$.lastName").value("Смирнов"))
                    .andExpect(jsonPath("$.completedSessions").value(0))
                    .andExpect(jsonPath("$.createdAt").isNotEmpty());
        }

        @Test
        @DisplayName("несуществующий ID → 404")
        void randomId_shouldReturn404() throws Exception {
            String token = registerAndGetToken("user@test.com");

            mockMvc.perform(get("/api/clients/{id}", UUID.randomUUID())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("чужой клиент → 404 (не даём знать о существовании)")
        void otherUsersClient_shouldReturn404() throws Exception {
            String tokenA = registerAndGetToken("userA@test.com");
            String tokenB = registerAndGetToken("userB@test.com");
            String clientId = createClient(tokenA, validClientRequest("Чужой", null));

            mockMvc.perform(get("/api/clients/{id}", clientId)
                            .header("Authorization", "Bearer " + tokenB))
                    .andExpect(status().isNotFound());
        }
    }

    // ── PUT /api/clients/{id} ──────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /api/clients/{id}")
    class UpdateClient {

        @Test
        @DisplayName("обновление полей → 200 с новыми данными")
        void update_shouldReturn200() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token, validClientRequest("Старое", "Имя"));

            ClientRequest update = validClientRequest("Новое", "Имя");
            update.setPhone("+79001234567");
            update.setInitialRequest("Новый запрос");

            mockMvc.perform(put("/api/clients/{id}", clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(update))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Новое"))
                    .andExpect(jsonPath("$.phone").value("+79001234567"))
                    .andExpect(jsonPath("$.initialRequest").value("Новый запрос"));
        }

        @Test
        @DisplayName("пустое имя при обновлении → 400")
        void blankName_shouldReturn400() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token, validClientRequest("Иван", null));

            ClientRequest update = validClientRequest("", null);

            mockMvc.perform(put("/api/clients/{id}", clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(update))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("чужой клиент → 404")
        void otherUsersClient_shouldReturn404() throws Exception {
            String tokenA = registerAndGetToken("userA@test.com");
            String tokenB = registerAndGetToken("userB@test.com");
            String clientId = createClient(tokenA, validClientRequest("Иван", null));

            mockMvc.perform(put("/api/clients/{id}", clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(validClientRequest("Попытка", null)))
                            .header("Authorization", "Bearer " + tokenB))
                    .andExpect(status().isNotFound());
        }
    }

    // ── PATCH /api/clients/{id}/archive ───────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/clients/{id}/archive")
    class ArchiveClient {

        @Test
        @DisplayName("архивировать клиента → 200")
        void archive_shouldReturn200() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token, validClientRequest("Иван", null));

            mockMvc.perform(patch("/api/clients/{id}/archive", clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"archive\":true}")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());

            // Клиент не в активном списке
            mockMvc.perform(get("/api/clients")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("разархивировать клиента → 200, снова в списке")
        void unarchive_shouldReturn200() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token, validClientRequest("Иван", null));

            // Архивируем
            mockMvc.perform(patch("/api/clients/{id}/archive", clientId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"archive\":true}")
                    .header("Authorization", "Bearer " + token));

            // Разархивируем
            mockMvc.perform(patch("/api/clients/{id}/archive", clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"archive\":false}")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/clients")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("несуществующий клиент → 404")
        void unknownClient_shouldReturn404() throws Exception {
            String token = registerAndGetToken("user@test.com");

            mockMvc.perform(patch("/api/clients/{id}/archive", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"archive\":true}")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }
    }

    // ── PATCH /api/clients/{id}/progress ──────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/clients/{id}/progress")
    class UpdateProgress {

        @Test
        @DisplayName("обновить прогресс → 200")
        void update_shouldReturn200() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token, validClientRequest("Иван", null));

            mockMvc.perform(patch("/api/clients/{id}/progress", clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"overallProgress\":\"Хорошая динамика\"}")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());

            // Убедиться что прогресс сохранился
            mockMvc.perform(get("/api/clients/{id}", clientId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.overallProgress").value("Хорошая динамика"));
        }

        @Test
        @DisplayName("пустой прогресс → 400")
        void blankProgress_shouldReturn400() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token, validClientRequest("Иван", null));

            mockMvc.perform(patch("/api/clients/{id}/progress", clientId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"overallProgress\":\"\"}")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── GET /api/clients/{id}/sessions ────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/clients/{id}/sessions")
    class GetClientSessions {

        @Test
        @DisplayName("нет сессий → 200 []")
        void noSessions_shouldReturnEmptyList() throws Exception {
            String token = registerAndGetToken("user@test.com");
            String clientId = createClient(token, validClientRequest("Иван", null));

            mockMvc.perform(get("/api/clients/{id}/sessions", clientId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("несуществующий клиент → 404")
        void unknownClient_shouldReturn404() throws Exception {
            String token = registerAndGetToken("user@test.com");

            mockMvc.perform(get("/api/clients/{id}/sessions", UUID.randomUUID())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("чужой клиент → 404")
        void otherUsersClient_shouldReturn404() throws Exception {
            String tokenA = registerAndGetToken("userA@test.com");
            String tokenB = registerAndGetToken("userB@test.com");
            String clientId = createClient(tokenA, validClientRequest("Иван", null));

            mockMvc.perform(get("/api/clients/{id}/sessions", clientId)
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

    private String createClient(String token, ClientRequest req) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }

    private ClientRequest validClientRequest(String firstName, String lastName) {
        ClientRequest req = new ClientRequest();
        req.setFirstName(firstName);
        req.setLastName(lastName);
        return req;
    }

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}
