package com.vogulev.regreso.ai;

import com.vogulev.regreso.exception.AiProviderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("GeminiProvider — unit тесты с MockRestServiceServer")
class GeminiProviderTest {

    private static final String API_URL = "http://localhost/gemini";
    private static final String API_KEY = "test-key";
    private static final String FULL_URL = API_URL + "?key=" + API_KEY;

    MockRestServiceServer mockServer;
    GeminiProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        provider = new GeminiProvider(builder, API_KEY, API_URL);
    }

    @Test
    @DisplayName("корректный ответ → возвращает текст из candidates[0].content.parts[0].text")
    void validResponse_returnsText() {
        String responseJson = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [{"text": "Сгенерированное саммари"}]
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(FULL_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        String result = provider.generateSummary("Тестовый промпт");

        assertThat(result).isEqualTo("Сгенерированное саммари");
        mockServer.verify();
    }

    @Test
    @DisplayName("HTTP 400 от Gemini → AiProviderException")
    void http400_throwsAiProviderException() {
        mockServer.expect(requestTo(FULL_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("{\"error\":\"bad request\"}"));

        assertThatThrownBy(() -> provider.generateSummary("Промпт"))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("400");
    }

    @Test
    @DisplayName("HTTP 429 от Gemini → AiProviderException")
    void http429_throwsAiProviderException() {
        mockServer.expect(requestTo(FULL_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("{\"error\":\"quota exceeded\"}"));

        assertThatThrownBy(() -> provider.generateSummary("Промпт"))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("429");
    }

    @Test
    @DisplayName("HTTP 500 от Gemini → AiProviderException")
    void http500_throwsAiProviderException() {
        mockServer.expect(requestTo(FULL_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"server error\"}"));

        assertThatThrownBy(() -> provider.generateSummary("Промпт"))
                .isInstanceOf(AiProviderException.class);
    }

    @Test
    @DisplayName("пустой список candidates → AiProviderException")
    void emptyCandidates_throwsAiProviderException() {
        String responseJson = """
                {"candidates": []}
                """;

        mockServer.expect(requestTo(FULL_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.generateSummary("Промпт"))
                .isInstanceOf(AiProviderException.class);
    }
}
