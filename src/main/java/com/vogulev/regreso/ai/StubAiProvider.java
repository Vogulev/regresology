package com.vogulev.regreso.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "stub")
public class StubAiProvider implements AiProvider {

    @Override
    public String generateSummary(String prompt) {
        return "AI саммари недоступно (stub режим)";
    }
}
