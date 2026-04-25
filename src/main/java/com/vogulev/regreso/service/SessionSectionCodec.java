package com.vogulev.regreso.service;

import com.vogulev.regreso.dto.SessionSectionDto;
import com.vogulev.regreso.entity.Session;
import com.vogulev.regreso.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SessionSectionCodec {

    private static final TypeReference<List<SessionSectionDto>> SECTION_LIST_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public List<SessionSectionDto> defaultSections() {
        List<SessionSectionDto> sections = new ArrayList<>();
        sections.add(defaultSection("EMOTIONAL_STATE_START", "Эмоциональное состояние в начале сеанса", 1));
        sections.add(defaultSection("DISORDERS_BEFORE_SESSION", "Наличие расстройств или проблем перед началом сеанса", 2));
        sections.add(defaultSection("CONCERN", "Что Вас беспокоит?", 3));
        sections.add(defaultSection("FIRST_APPEARANCE", "Когда эта проблема впервые появилась?", 4));
        sections.add(defaultSection("TRIGGER_SITUATION", "Вспомните ситуацию, когда ваша проблема проявлялась?", 5));
        sections.add(defaultSection("THOUGHTS_AND_EMOTIONS", "Какие мысли, чувства, эмоции, состояния возникают?", 6));
        sections.add(defaultSection("BODY_DISCOMFORT", "Где в теле в этот момент возникает дискомфорт?", 7));
        sections.add(defaultSection("SELF_IMAGE", "Какой/какая Вы там?", 8));
        sections.add(defaultSection("DESIRED_CHANGE_RESULT", "Представьте, что изменения уже наступили. Что Вам это даст?", 9));
        sections.add(defaultSection("SESSION_GOAL", "Цель на сеанс", 10));
        sections.add(defaultSection("POWER_PLACE", "Место силы", 11));
        sections.add(defaultSection("SPIRITUAL_GUIDE", "Духовный наставник", 12));
        sections.add(defaultSection("HIGHER_SELF", "Высшее Я", 13));
        sections.add(defaultSection("LIFE_1", "1 Жизнь", 14));
        sections.add(defaultSection("LIFE_2", "2 Жизнь", 15));
        sections.add(defaultSection("RESULT", "Итог", 16));
        sections.add(defaultSection("EMOTIONAL_STATE_END", "Состояние регрессанта в конце сессии", 17));
        return sections;
    }

    public List<SessionSectionDto> instantiateTemplateSections(String templateJson) {
        List<SessionSectionDto> template = (templateJson == null || templateJson.isBlank())
                ? defaultSections()
                : deserialize(templateJson);

        return template.stream()
                .map(section -> SessionSectionDto.builder()
                        .id(UUID.randomUUID())
                        .code(section.getCode())
                        .title(section.getTitle())
                        .content(section.getContent())
                        .isDefault(section.getIsDefault())
                        .position(section.getPosition())
                        .build())
                .toList();
    }

    public List<SessionSectionDto> resolveSections(Session session) {
        if (session.getSectionsJson() != null && !session.getSectionsJson().isBlank()) {
            return deserialize(session.getSectionsJson());
        }
        return mergeLegacyFields(defaultSections(), session);
    }

    public String serialize(List<SessionSectionDto> sections) {
        try {
            return objectMapper.writeValueAsString(sections);
        } catch (JacksonException e) {
            throw new BusinessException("Не удалось сохранить секции сессии");
        }
    }

    public List<SessionSectionDto> normalize(List<SessionSectionDto> sections) {
        if (sections == null || sections.isEmpty()) {
            return defaultSections();
        }

        List<SessionSectionDto> normalized = new ArrayList<>();
        for (int i = 0; i < sections.size(); i++) {
            SessionSectionDto section = sections.get(i);
            String title = normalizeText(section.getTitle());
            if (title == null) {
                throw new BusinessException("Название секции обязательно");
            }

            normalized.add(SessionSectionDto.builder()
                    .id(section.getId() != null ? section.getId() : UUID.randomUUID())
                    .code(normalizeCode(section.getCode()))
                    .title(title)
                    .content(normalizeText(section.getContent()))
                    .isDefault(Boolean.TRUE.equals(section.getIsDefault()))
                    .position(i + 1)
                    .build());
        }

        return normalized;
    }

    public List<SessionSectionDto> mergeLegacyFields(List<SessionSectionDto> sections, Session session) {
        Map<String, String> legacyValues = new LinkedHashMap<>();
        legacyValues.put("EMOTIONAL_STATE_START", normalizeText(session.getPreSessionState()));
        legacyValues.put("DISORDERS_BEFORE_SESSION", normalizeText(session.getInductionNotes()));
        legacyValues.put("CONCERN", normalizeText(session.getPreSessionRequest()));
        legacyValues.put("POWER_PLACE", normalizeText(session.getRegressionSetting()));
        legacyValues.put("RESULT", firstNonBlank(session.getKeyInsights(), session.getIntegrationNotes()));
        legacyValues.put("EMOTIONAL_STATE_END", normalizeText(session.getPostSessionState()));

        return sections.stream()
                .map(section -> SessionSectionDto.builder()
                        .id(section.getId())
                        .code(section.getCode())
                        .title(section.getTitle())
                        .content(legacyValues.containsKey(section.getCode())
                                ? legacyValues.get(section.getCode())
                                : section.getContent())
                        .isDefault(section.getIsDefault())
                        .position(section.getPosition())
                        .build())
                .toList();
    }

    public void syncLegacyFields(Session session, List<SessionSectionDto> sections) {
        Map<String, String> values = sections.stream()
                .filter(section -> section.getCode() != null)
                .collect(LinkedHashMap::new,
                        (map, section) -> map.put(section.getCode(), normalizeText(section.getContent())),
                        Map::putAll);

        session.setPreSessionState(values.get("EMOTIONAL_STATE_START"));
        session.setInductionNotes(values.get("DISORDERS_BEFORE_SESSION"));
        session.setPreSessionRequest(values.get("CONCERN"));
        session.setRegressionSetting(values.get("POWER_PLACE"));
        session.setKeyInsights(values.get("RESULT"));
        session.setPostSessionState(values.get("EMOTIONAL_STATE_END"));
    }

    public String buildPromptFromSections(Session session) {
        return resolveSections(session).stream()
                .filter(section -> section.getContent() != null && !section.getContent().isBlank())
                .map(section -> section.getTitle() + ": " + section.getContent())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    private List<SessionSectionDto> deserialize(String json) {
        try {
            return normalize(objectMapper.readValue(json, SECTION_LIST_TYPE));
        } catch (JacksonException e) {
            throw new BusinessException("Не удалось прочитать секции сессии");
        }
    }

    private SessionSectionDto defaultSection(String code, String title, int position) {
        return SessionSectionDto.builder()
                .id(UUID.randomUUID())
                .code(code)
                .title(title)
                .isDefault(true)
                .position(position)
                .build();
    }

    private String normalizeCode(String code) {
        String normalized = normalizeText(code);
        return normalized != null ? normalized : null;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String firstNonBlank(String primary, String fallback) {
        String normalizedPrimary = normalizeText(primary);
        return normalizedPrimary != null ? normalizedPrimary : normalizeText(fallback);
    }
}
