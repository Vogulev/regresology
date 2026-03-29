package com.vogulev.regreso.controller;

import tools.jackson.databind.json.JsonMapper;
import com.vogulev.regreso.BaseIntegrationTest;
import com.vogulev.regreso.dto.request.MaterialRequest;
import com.vogulev.regreso.dto.request.RegisterRequest;
import com.vogulev.regreso.repository.BookingSettingsRepository;
import com.vogulev.regreso.repository.ClientRepository;
import com.vogulev.regreso.repository.MaterialRepository;
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

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("MaterialController — интеграционные тесты")
class MaterialControllerTest extends BaseIntegrationTest {

    @Autowired JsonMapper objectMapper;
    @Autowired BookingSettingsRepository bookingSettingsRepository;
    @Autowired MaterialRepository materialRepository;
    @Autowired ReminderRepository reminderRepository;
    @Autowired SessionRepository sessionRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired PractitionerRepository practitionerRepository;

    @BeforeEach
    void cleanUp() {
        bookingSettingsRepository.deleteAll();
        reminderRepository.deleteAll();
        sessionRepository.deleteAll();
        materialRepository.deleteAll();
        clientRepository.deleteAll();
        practitionerRepository.deleteAll();
    }

    // ── GET /api/materials ────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/materials")
    class GetMaterials {

        @Test
        @DisplayName("без токена → 401")
        void noToken_returns401() throws Exception {
            mockMvc.perform(get("/api/materials"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("пустой список → 200 []")
        void empty_returns200() throws Exception {
            String token = registerAndGetToken("mat@test.com");

            mockMvc.perform(get("/api/materials")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("только неархивные материалы по умолчанию")
        void defaultExcludesArchived() throws Exception {
            String token = registerAndGetToken("mat@test.com");
            createMaterial(token, "Активный", "practice");
            String id = createMaterialAndGetId(token, "Архивный", "book");
            archiveMaterial(token, id);

            mockMvc.perform(get("/api/materials")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].title").value("Активный"));
        }

        @Test
        @DisplayName("includeArchived=true → возвращает все")
        void includeArchived_returnsAll() throws Exception {
            String token = registerAndGetToken("mat@test.com");
            createMaterial(token, "Активный", "practice");
            String id = createMaterialAndGetId(token, "Архивный", "book");
            archiveMaterial(token, id);

            mockMvc.perform(get("/api/materials?includeArchived=true")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("изоляция между практиками")
        void practitionerIsolation() throws Exception {
            String tokenA = registerAndGetToken("matA@test.com");
            String tokenB = registerAndGetToken("matB@test.com");
            createMaterial(tokenA, "Материал А", "meditation");

            mockMvc.perform(get("/api/materials")
                            .header("Authorization", "Bearer " + tokenB))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ── POST /api/materials ───────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/materials")
    class CreateMaterial {

        @Test
        @DisplayName("валидный запрос → 201 с материалом")
        void valid_returns201() throws Exception {
            String token = registerAndGetToken("mat@test.com");
            MaterialRequest req = buildRequest("Дыхательная практика", "practice");

            mockMvc.perform(post("/api/materials")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.title").value("Дыхательная практика"))
                    .andExpect(jsonPath("$.materialType").value("practice"))
                    .andExpect(jsonPath("$.isArchived").value(false));
        }

        @Test
        @DisplayName("multipart с файлом → 201 и метаданные файла в ответе")
        void multipartWithFile_returns201() throws Exception {
            String token = registerAndGetToken("mat-file@test.com");
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "guide.pdf",
                    "application/pdf",
                    "pdf-content".getBytes()
            );

            mockMvc.perform(multipart("/api/materials")
                            .file(file)
                            .param("title", "Методичка")
                            .param("description", "PDF-файл для клиента")
                            .param("materialType", "guide")
                            .param("content", "Краткое описание")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("Методичка"))
                    .andExpect(jsonPath("$.fileName").value("guide.pdf"))
                    .andExpect(jsonPath("$.mimeType").value("application/pdf"))
                    .andExpect(jsonPath("$.fileSizeBytes").value(11))
                    .andExpect(jsonPath("$.fileUrl", startsWith("/api/files/materials/")));
        }

        @Test
        @DisplayName("без title → 400")
        void missingTitle_returns400() throws Exception {
            String token = registerAndGetToken("mat@test.com");

            mockMvc.perform(post("/api/materials")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"materialType\":\"book\"}")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("без токена → 401")
        void noToken_returns401() throws Exception {
            mockMvc.perform(post("/api/materials")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"T\",\"materialType\":\"book\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── PUT /api/materials/{id} ───────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /api/materials/{id}")
    class UpdateMaterial {

        @Test
        @DisplayName("обновление → 200 с новыми данными")
        void update_returns200() throws Exception {
            String token = registerAndGetToken("mat@test.com");
            String id = createMaterialAndGetId(token, "Старое название", "book");

            MaterialRequest req = buildRequest("Новое название", "article");
            req.setContent("Обновлённый контент");

            mockMvc.perform(put("/api/materials/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Новое название"))
                    .andExpect(jsonPath("$.materialType").value("article"))
                    .andExpect(jsonPath("$.content").value("Обновлённый контент"));
        }

        @Test
        @DisplayName("multipart обновление заменяет файл")
        void updateMultipart_replacesFile() throws Exception {
            String token = registerAndGetToken("mat-update-file@test.com");
            String id = createMaterialAndGetId(token, "Старое название", "book");
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "new-guide.pdf",
                    "application/pdf",
                    "updated-content".getBytes()
            );

            mockMvc.perform(multipart("/api/materials/{id}", id)
                            .file(file)
                            .param("title", "Новое название")
                            .param("description", "Новое описание")
                            .param("materialType", "article")
                            .param("content", "Обновлённый контент")
                            .header("Authorization", "Bearer " + token)
                            .with(request -> {
                                request.setMethod("PUT");
                                return request;
                            }))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Новое название"))
                    .andExpect(jsonPath("$.fileName").value("new-guide.pdf"))
                    .andExpect(jsonPath("$.mimeType").value("application/pdf"))
                    .andExpect(jsonPath("$.fileUrl", startsWith("/api/files/materials/")));
        }

        @Test
        @DisplayName("чужой материал → 404")
        void otherUserMaterial_returns404() throws Exception {
            String tokenA = registerAndGetToken("matA@test.com");
            String tokenB = registerAndGetToken("matB@test.com");
            String id = createMaterialAndGetId(tokenA, "Материал А", "book");

            mockMvc.perform(put("/api/materials/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(buildRequest("Взлом", "book")))
                            .header("Authorization", "Bearer " + tokenB))
                    .andExpect(status().isNotFound());
        }
    }

    // ── DELETE /api/materials/{id} (архивирование) ────────────────────────────

    @Nested
    @DisplayName("DELETE /api/materials/{id}")
    class ArchiveMaterial {

        @Test
        @DisplayName("архивирование → 200, материал помечается is_archived=true")
        void archive_returns200() throws Exception {
            String token = registerAndGetToken("mat@test.com");
            String id = createMaterialAndGetId(token, "К архиву", "other");

            mockMvc.perform(delete("/api/materials/{id}", id)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());

            // Проверяем что материал помечен как архивный (не удалён)
            mockMvc.perform(get("/api/materials?includeArchived=true")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].isArchived").value(true));
        }

        @Test
        @DisplayName("архивированный материал не в основном списке")
        void archivedNotInMainList() throws Exception {
            String token = registerAndGetToken("mat@test.com");
            String id = createMaterialAndGetId(token, "К архиву", "other");
            archiveMaterial(token, id);

            mockMvc.perform(get("/api/materials")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("несуществующий → 404")
        void notFound_returns404() throws Exception {
            String token = registerAndGetToken("mat@test.com");

            mockMvc.perform(delete("/api/materials/{id}", UUID.randomUUID())
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

    private void createMaterial(String token, String title, String type) throws Exception {
        mockMvc.perform(post("/api/materials")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(buildRequest(title, type)))
                .header("Authorization", "Bearer " + token));
    }

    private String createMaterialAndGetId(String token, String title, String type) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/materials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(buildRequest(title, type)))
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private void archiveMaterial(String token, String id) throws Exception {
        mockMvc.perform(delete("/api/materials/{id}", id)
                .header("Authorization", "Bearer " + token));
    }

    private MaterialRequest buildRequest(String title, String type) {
        MaterialRequest req = new MaterialRequest();
        req.setTitle(title);
        req.setMaterialType(type);
        req.setDescription("Описание");
        return req;
    }

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}
