package com.vogulev.regreso.controller;

import tools.jackson.databind.json.JsonMapper;
import com.vogulev.regreso.BaseIntegrationTest;
import com.vogulev.regreso.dto.request.RegisterRequest;
import com.vogulev.regreso.entity.Client;
import com.vogulev.regreso.entity.Practitioner;
import com.vogulev.regreso.entity.Session;
import com.vogulev.regreso.repository.BookingSettingsRepository;
import com.vogulev.regreso.repository.ClientRepository;
import com.vogulev.regreso.repository.PractitionerRepository;
import com.vogulev.regreso.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("PublicBookingController — интеграционные тесты")
class PublicBookingControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired JsonMapper objectMapper;
    @Autowired PractitionerRepository practitionerRepository;
    @Autowired BookingSettingsRepository bookingSettingsRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired SessionRepository sessionRepository;

    @BeforeEach
    void cleanUp() {
        bookingSettingsRepository.deleteAll();
        sessionRepository.deleteAll();
        clientRepository.deleteAll();
        practitionerRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/public/booking/{slug}/slots учитывает рабочие часы, перерывы и занятые окна")
    void getAvailableSlots_shouldRespectCustomAvailability() throws Exception {
        String email = "public-booking@test.com";
        String token = registerAndGetToken(email);
        Practitioner practitioner = practitionerRepository.findByEmail(email).orElseThrow();

        ZoneId zone = ZoneId.of("Europe/Moscow");
        LocalDate targetDate = LocalDate.now(zone).plusDays(1);
        int targetDay = targetDate.getDayOfWeek().getValue();

        mockMvc.perform(put("/api/settings/booking")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "isEnabled": true,
                                  "slug": "test-public-slots",
                                  "defaultDurationMin": 60,
                                  "bufferMin": 0,
                                  "advanceDays": 30,
                                  "availabilityMode": "CUSTOM",
                                  "weeklyAvailability": [
                                    {
                                      "dayOfWeek": %d,
                                      "isWorkingDay": true,
                                      "startTime": "10:00",
                                      "endTime": "16:00",
                                      "slotIntervalMin": 60,
                                      "breaks": [
                                        { "startTime": "12:00", "endTime": "13:00" }
                                      ]
                                    }
                                  ]
                                }
                                """.formatted(targetDay)))
                .andExpect(status().isOk());

        Client client = clientRepository.save(Client.builder()
                .practitioner(practitioner)
                .firstName("Иван")
                .phone("+79990000000")
                .build());

        sessionRepository.save(Session.builder()
                .practitioner(practitioner)
                .client(client)
                .scheduledAt(targetDate.atTime(14, 0).atZone(zone).toOffsetDateTime())
                .durationMin(60)
                .sessionNumber(1)
                .build());

        mockMvc.perform(get("/api/public/booking/test-public-slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.advanceDays").value(30));

        mockMvc.perform(get("/api/public/booking/test-public-slots/slots")
                        .param("date", targetDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots.length()").value(4))
                .andExpect(jsonPath("$.slots[0].startsAt").value(targetDate + "T10:00:00+03:00"))
                .andExpect(jsonPath("$.slots[1].startsAt").value(targetDate + "T11:00:00+03:00"))
                .andExpect(jsonPath("$.slots[2].startsAt").value(targetDate + "T13:00:00+03:00"))
                .andExpect(jsonPath("$.slots[3].startsAt").value(targetDate + "T15:00:00+03:00"));
    }

    @Test
    @DisplayName("GET /api/public/booking/{slug}/slots учитывает глобальный перерыв между сессиями")
    void getAvailableSlots_shouldRespectBufferBetweenSessions() throws Exception {
        String email = "public-buffer@test.com";
        String token = registerAndGetToken(email);
        Practitioner practitioner = practitionerRepository.findByEmail(email).orElseThrow();

        ZoneId zone = ZoneId.of("Europe/Moscow");
        LocalDate targetDate = LocalDate.now(zone).plusDays(1);
        int targetDay = targetDate.getDayOfWeek().getValue();

        mockMvc.perform(put("/api/settings/booking")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "isEnabled": true,
                                  "slug": "test-buffer-slots",
                                  "defaultDurationMin": 60,
                                  "bufferMin": 10,
                                  "advanceDays": 30,
                                  "availabilityMode": "CUSTOM",
                                  "weeklyAvailability": [
                                    {
                                      "dayOfWeek": %d,
                                      "isWorkingDay": true,
                                      "startTime": "10:00",
                                      "endTime": "14:00",
                                      "slotIntervalMin": 10,
                                      "breaks": []
                                    }
                                  ]
                                }
                                """.formatted(targetDay)))
                .andExpect(status().isOk());

        Client client = clientRepository.save(Client.builder()
                .practitioner(practitioner)
                .firstName("Мария")
                .phone("+79990000001")
                .build());

        sessionRepository.save(Session.builder()
                .practitioner(practitioner)
                .client(client)
                .scheduledAt(targetDate.atTime(11, 0).atZone(zone).toOffsetDateTime())
                .durationMin(60)
                .sessionNumber(1)
                .build());

        mockMvc.perform(get("/api/public/booking/test-buffer-slots/slots")
                        .param("date", targetDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots[0].startsAt").value(targetDate + "T12:10:00+03:00"))
                .andExpect(jsonPath("$.slots[1].startsAt").value(targetDate + "T12:20:00+03:00"))
                .andExpect(jsonPath("$.slots[2].startsAt").value(targetDate + "T12:30:00+03:00"));
    }

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
