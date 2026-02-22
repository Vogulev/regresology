package com.vogulev.regreso.controller;

import tools.jackson.databind.json.JsonMapper;
import com.vogulev.regreso.BaseIntegrationTest;
import com.vogulev.regreso.dto.request.RegisterRequest;
import com.vogulev.regreso.repository.ClientRepository;
import com.vogulev.regreso.repository.PractitionerRepository;
import com.vogulev.regreso.repository.ReminderRepository;
import com.vogulev.regreso.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("SettingsController — интеграционные тесты")
class SettingsControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired JsonMapper objectMapper;
    @Autowired PractitionerRepository practitionerRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired SessionRepository sessionRepository;
    @Autowired ReminderRepository reminderRepository;

    @BeforeEach
    void cleanUp() {
        reminderRepository.deleteAll();
        sessionRepository.deleteAll();
        clientRepository.deleteAll();
        practitionerRepository.deleteAll();
    }

    @Test
    @DisplayName("без токена → 401")
    void noToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/settings/telegram/link"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/settings/telegram/link → 200 с ссылкой на бота")
    void getTelegramLink_shouldReturn200WithLink() throws Exception {
        String token = registerAndGetToken("user@test.com");

        mockMvc.perform(get("/api/settings/telegram/link")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.botUrl").value(containsString("t.me/")))
                .andExpect(jsonPath("$.botUrl").value(containsString("PRACTITIONER_")))
                .andExpect(jsonPath("$.connected").value(false))
                .andExpect(jsonPath("$.telegramChatId").doesNotExist());
    }

    @Test
    @DisplayName("ссылка содержит корректный username бота")
    void telegramLink_containsBotUsername() throws Exception {
        String token = registerAndGetToken("user@test.com");

        mockMvc.perform(get("/api/settings/telegram/link")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.botUrl").value(containsString("TestBot")));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String registerAndGetToken(String email) throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setPassword("password123");
        req.setFirstName("Тест");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }
}
