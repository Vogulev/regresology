package com.vogulev.regreso.controller;

import tools.jackson.databind.json.JsonMapper;
import com.vogulev.regreso.BaseIntegrationTest;
import com.vogulev.regreso.dto.request.RegisterRequest;
import com.vogulev.regreso.repository.BookingSettingsRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("SettingsController — интеграционные тесты")
class SettingsControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired JsonMapper objectMapper;
    @Autowired PractitionerRepository practitionerRepository;
    @Autowired BookingSettingsRepository bookingSettingsRepository;
    @Autowired ClientRepository clientRepository;
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

    @Test
    @DisplayName("PUT /api/settings/booking сохраняет кастомное расписание")
    void updateBookingSettings_shouldPersistCustomAvailability() throws Exception {
        String token = registerAndGetToken("booking@test.com");

        mockMvc.perform(put("/api/settings/booking")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "isEnabled": true,
                                  "slug": "custom-schedule",
                                  "defaultDurationMin": 90,
                                  "bufferMin": 15,
                                  "advanceDays": 21,
                                  "requireIntakeForm": false,
                                  "welcomeMessage": "Добро пожаловать",
                                  "availabilityMode": "CUSTOM",
                                  "weeklyAvailability": [
                                    {
                                      "dayOfWeek": 1,
                                      "isWorkingDay": true,
                                      "startTime": "10:00",
                                      "endTime": "18:00",
                                      "slotIntervalMin": 60,
                                      "breaks": [
                                        { "startTime": "13:00", "endTime": "14:00" }
                                      ]
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availabilityMode").value("CUSTOM"))
                .andExpect(jsonPath("$.weeklyAvailability[0].dayOfWeek").value(1))
                .andExpect(jsonPath("$.weeklyAvailability[0].breaks[0].startTime").value("13:00"))
                .andExpect(jsonPath("$.advanceDays").value(21));

        mockMvc.perform(get("/api/settings/booking")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("custom-schedule"))
                .andExpect(jsonPath("$.availabilityMode").value("CUSTOM"))
                .andExpect(jsonPath("$.weeklyAvailability[0].endTime").value("18:00"))
                .andExpect(jsonPath("$.weeklyAvailability[0].breaks[0].endTime").value("14:00"));
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
