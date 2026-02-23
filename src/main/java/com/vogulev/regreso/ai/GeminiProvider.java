package com.vogulev.regreso.ai;

import com.vogulev.regreso.exception.AiProviderException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "gemini")
public class GeminiProvider implements AiProvider {

    private final RestClient restClient;
    private final String apiKey;
    private final String apiUrl;

    public GeminiProvider(
            RestClient.Builder builder,
            @Value("${ai.gemini.api-key}") String apiKey,
            @Value("${ai.gemini.api-url:https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent}") String apiUrl) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.restClient = builder.build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public String generateSummary(String prompt) {
        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "maxOutputTokens", 500,
                        "temperature", 0.3
                )
        );

        try {
            Map<String, Object> response = restClient.post()
                    .uri(apiUrl + "?key=" + apiKey)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new AiProviderException("Gemini вернул пустой ответ");
            }

            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                throw new AiProviderException("Gemini не вернул candidates");
            }

            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            return (String) parts.get(0).get("text");

        } catch (RestClientResponseException e) {
            log.warn("Gemini HTTP ошибка: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AiProviderException("Gemini вернул ошибку: " + e.getStatusCode(), e);
        }
    }
}
