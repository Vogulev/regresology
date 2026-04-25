package com.vogulev.regreso.ai;

import com.vogulev.regreso.entity.Client;
import com.vogulev.regreso.entity.Session;
import com.vogulev.regreso.event.SessionCompletedEvent;
import com.vogulev.regreso.exception.BusinessException;
import com.vogulev.regreso.exception.ResourceNotFoundException;
import com.vogulev.regreso.exception.TooManyRequestsException;
import com.vogulev.regreso.repository.ClientRepository;
import com.vogulev.regreso.repository.SessionRepository;
import com.vogulev.regreso.service.SessionSectionCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiSummaryService {

    private static final String SESSION_SYSTEM_PROMPT = """
            Ты помощник регрессолога. На основе протокола сессии напиши краткое саммари \
            (5-8 предложений) на русском языке.
            Стиль: профессиональный, от третьего лица. Без воды. Только суть.
            Структура: с чем пришёл клиент → куда ушёл в регрессии → \
            ключевые инсайты и эмоции → что проработал → состояние после и план.""";

    private static final String CLIENT_SYSTEM_PROMPT = """
            Ты помощник регрессолога. На основе всех сессий клиента напиши \
            саммари динамики работы (7-10 предложений) на русском языке.
            Опиши: с чем пришёл изначально, какие темы прорабатывались, \
            какие инсайты были ключевыми, как изменился клиент.""";

    private final AiProvider aiProvider;
    private final SessionRepository sessionRepository;
    private final ClientRepository clientRepository;
    private final SessionSectionCodec sessionSectionCodec;

    @Value("${ai.summary.min-prompt-length:100}")
    private int minPromptLength;

    @Value("${ai.summary.regenerate-cooldown-minutes:60}")
    private int cooldownMinutes;

    // ── Session summary ───────────────────────────────────────────────────────

    /**
     * Проверяет cooldown и запускает асинхронную генерацию саммари сессии.
     */
    public void triggerSessionSummary(UUID sessionId, UUID practitionerId) {
        Session session = sessionRepository.findByIdAndPractitionerId(sessionId, practitionerId)
                .orElseThrow(() -> new ResourceNotFoundException("Сессия не найдена"));

        checkCooldown(session.getAiSummaryGeneratedAt());
        generateSessionSummaryAsync(sessionId);
    }

    /**
     * Автозапуск при завершении сессии.
     */
    @EventListener
    @Async("aiExecutor")
    @Transactional
    public void onSessionCompleted(SessionCompletedEvent event) {
        doGenerateSessionSummary(event.getSessionId());
    }

    /**
     * Ручной запуск (из triggerSessionSummary).
     */
    @Async("aiExecutor")
    @Transactional
    public void generateSessionSummaryAsync(UUID sessionId) {
        doGenerateSessionSummary(sessionId);
    }

    private void doGenerateSessionSummary(UUID sessionId) {
        Session session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            log.warn("AiSummaryService: сессия {} не найдена", sessionId);
            return;
        }

        String prompt = buildSessionPrompt(session);
        if (prompt.length() < minPromptLength) {
            log.debug("AiSummaryService: prompt слишком короткий ({} символов), пропускаем", prompt.length());
            return;
        }

        try {
            String summary = aiProvider.generateSummary(SESSION_SYSTEM_PROMPT + "\n\n" + prompt);
            session.setAiSummary(summary);
            session.setAiSummaryGeneratedAt(OffsetDateTime.now());
            sessionRepository.save(session);
            log.info("AiSummaryService: саммари сессии {} сгенерировано", sessionId);
        } catch (Exception e) {
            log.warn("AiSummaryService: ошибка генерации саммари сессии {}: {}", sessionId, e.getMessage());
        }
    }

    private String buildSessionPrompt(Session session) {
        StringBuilder sb = new StringBuilder();
        String sectionPrompt = sessionSectionCodec.buildPromptFromSections(session);
        if (!sectionPrompt.isBlank()) {
            sb.append(sectionPrompt).append("\n");
        }
        if (session.getPreSessionRequest() != null && !session.getPreSessionRequest().isBlank()) {
            sb.append("Запрос клиента: ").append(session.getPreSessionRequest()).append("\n");
        }
        if (session.getRegressionTarget() != null || session.getRegressionPeriod() != null) {
            sb.append("Тип регрессии: ");
            if (session.getRegressionTarget() != null) sb.append(session.getRegressionTarget().name());
            if (session.getRegressionPeriod() != null) sb.append(" / ").append(session.getRegressionPeriod());
            sb.append("\n");
        }
        if (session.getRegressionSetting() != null && !session.getRegressionSetting().isBlank()) {
            sb.append("Обстановка: ").append(session.getRegressionSetting()).append("\n");
        }
        if (session.getKeyScenes() != null && !session.getKeyScenes().isBlank()) {
            sb.append("Ключевые сцены: ").append(session.getKeyScenes()).append("\n");
        }
        if (session.getKeyEmotions() != null && session.getKeyEmotions().length > 0) {
            sb.append("Эмоции: ").append(String.join(", ", Arrays.asList(session.getKeyEmotions()))).append("\n");
        }
        if (session.getKeyInsights() != null && !session.getKeyInsights().isBlank()) {
            sb.append("Инсайты: ").append(session.getKeyInsights()).append("\n");
        }
        if (session.getBlocksReleased() != null && !session.getBlocksReleased().isBlank()) {
            sb.append("Проработано: ").append(session.getBlocksReleased()).append("\n");
        }
        if (session.getPostSessionState() != null && !session.getPostSessionState().isBlank()) {
            sb.append("Состояние после: ").append(session.getPostSessionState()).append("\n");
        }
        if (session.getNextSessionPlan() != null && !session.getNextSessionPlan().isBlank()) {
            sb.append("План на следующую: ").append(session.getNextSessionPlan()).append("\n");
        }
        return sb.toString().trim();
    }

    // ── Client summary ────────────────────────────────────────────────────────

    /**
     * Проверяет cooldown и запускает асинхронную генерацию саммари клиента.
     */
    public void triggerClientSummary(UUID clientId, UUID practitionerId) {
        Client client = clientRepository.findByIdAndPractitionerId(clientId, practitionerId)
                .orElseThrow(() -> new ResourceNotFoundException("Клиент не найден"));

        checkCooldown(client.getAiOverallSummaryGeneratedAt());

        List<Session> sessions = sessionRepository.findTop10CompletedWithSummaryByClientId(clientId);
        if (sessions.size() < 2) {
            throw new BusinessException("Недостаточно сессий для саммари");
        }

        generateClientSummaryAsync(clientId);
    }

    @Async("aiExecutor")
    @Transactional
    public void generateClientSummaryAsync(UUID clientId) {
        List<Session> sessions = sessionRepository.findTop10CompletedWithSummaryByClientId(clientId);
        if (sessions.size() < 2) {
            log.warn("AiSummaryService: недостаточно сессий для клиента {}", clientId);
            return;
        }

        String prompt = buildClientPrompt(sessions);

        try {
            String summary = aiProvider.generateSummary(CLIENT_SYSTEM_PROMPT + "\n\n" + prompt);
            Client client = clientRepository.findById(clientId).orElse(null);
            if (client == null) return;
            client.setAiOverallSummary(summary);
            client.setAiOverallSummaryGeneratedAt(OffsetDateTime.now());
            clientRepository.save(client);
            log.info("AiSummaryService: саммари клиента {} сгенерировано", clientId);
        } catch (Exception e) {
            log.warn("AiSummaryService: ошибка генерации саммари клиента {}: {}", clientId, e.getMessage());
        }
    }

    private String buildClientPrompt(List<Session> sessions) {
        return sessions.stream()
                .map(s -> "Сессия от " + s.getScheduledAt().toLocalDate() + ":\n" + s.getAiSummary())
                .collect(Collectors.joining("\n\n"));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void checkCooldown(OffsetDateTime generatedAt) {
        if (generatedAt != null &&
                generatedAt.isAfter(OffsetDateTime.now().minusMinutes(cooldownMinutes))) {
            throw new TooManyRequestsException("Повторная генерация доступна через " + cooldownMinutes + " минут");
        }
    }
}
