package com.vogulev.regreso.ai;

import com.vogulev.regreso.entity.Client;
import com.vogulev.regreso.entity.Practitioner;
import com.vogulev.regreso.entity.Session;
import com.vogulev.regreso.exception.AiProviderException;
import com.vogulev.regreso.exception.BusinessException;
import com.vogulev.regreso.exception.TooManyRequestsException;
import com.vogulev.regreso.repository.ClientRepository;
import com.vogulev.regreso.repository.SessionRepository;
import com.vogulev.regreso.service.SessionSectionCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiSummaryService — unit тесты")
class AiSummaryServiceTest {

    @Mock
    AiProvider aiProvider;
    @Mock
    SessionRepository sessionRepository;
    @Mock
    ClientRepository clientRepository;
    @Mock
    SessionSectionCodec sessionSectionCodec;

    @InjectMocks
    AiSummaryService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "minPromptLength", 100);
        ReflectionTestUtils.setField(service, "cooldownMinutes", 60);
        lenient().when(sessionSectionCodec.buildPromptFromSections(any())).thenReturn("");
    }

    // ── doGenerateSessionSummary ───────────────────────────────────────────────

    @Nested
    @DisplayName("generateSessionSummaryAsync")
    class GenerateSessionSummaryAsync {

        @Test
        @DisplayName("короткий prompt (< 100 символов) → провайдер не вызывается")
        void shortPrompt_providerNotCalled() {
            Session session = sessionWithShortContent();
            when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

            service.generateSessionSummaryAsync(session.getId());

            verify(aiProvider, never()).generateSummary(anyString());
            verify(sessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("нормальный prompt → провайдер вызывается, результат сохраняется")
        void normalPrompt_summaryIsSaved() {
            Session session = sessionWithFullContent();
            when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
            when(aiProvider.generateSummary(anyString())).thenReturn("Сгенерированное саммари");

            service.generateSessionSummaryAsync(session.getId());

            ArgumentCaptor<Session> captor = ArgumentCaptor.forClass(Session.class);
            verify(sessionRepository).save(captor.capture());
            assertThat(captor.getValue().getAiSummary()).isEqualTo("Сгенерированное саммари");
            assertThat(captor.getValue().getAiSummaryGeneratedAt()).isNotNull();
        }

        @Test
        @DisplayName("AiProviderException → логируется, сессия не падает и не сохраняется")
        void providerException_sessionNotSaved() {
            Session session = sessionWithFullContent();
            when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
            when(aiProvider.generateSummary(anyString())).thenThrow(new AiProviderException("Ошибка провайдера"));

            // Не должно бросать исключение
            service.generateSessionSummaryAsync(session.getId());

            verify(sessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("сессия не найдена → тихо завершается")
        void sessionNotFound_noException() {
            UUID id = UUID.randomUUID();
            when(sessionRepository.findById(id)).thenReturn(Optional.empty());

            service.generateSessionSummaryAsync(id);

            verify(aiProvider, never()).generateSummary(anyString());
        }
    }

    // ── triggerSessionSummary cooldown ─────────────────────────────────────────

    @Nested
    @DisplayName("triggerSessionSummary — cooldown")
    class TriggerSessionSummaryCooldown {

        @Test
        @DisplayName("aiSummaryGeneratedAt < 60 мин назад → 429 TooManyRequestsException")
        void recentGeneratedAt_throwsTooManyRequests() {
            Session session = sessionWithFullContent();
            session.setAiSummaryGeneratedAt(OffsetDateTime.now().minusMinutes(30));
            UUID practitionerId = UUID.randomUUID();
            when(sessionRepository.findByIdAndPractitionerId(eq(session.getId()), eq(practitionerId)))
                    .thenReturn(Optional.of(session));

            assertThatThrownBy(() -> service.triggerSessionSummary(session.getId(), practitionerId))
                    .isInstanceOf(TooManyRequestsException.class);

            verify(aiProvider, never()).generateSummary(anyString());
        }

        @Test
        @DisplayName("aiSummaryGeneratedAt = null → cooldown не применяется")
        void nullGeneratedAt_noCooldown() {
            Session session = sessionWithFullContent();
            session.setAiSummaryGeneratedAt(null);
            UUID practitionerId = UUID.randomUUID();
            when(sessionRepository.findByIdAndPractitionerId(eq(session.getId()), eq(practitionerId)))
                    .thenReturn(Optional.of(session));

            // Не должно бросить TooManyRequestsException
            // (generateSessionSummaryAsync вызовется асинхронно, нам нужно только проверить cooldown)
            service.triggerSessionSummary(session.getId(), practitionerId);
            // Если дошли сюда без исключения — тест пройден
        }

        @Test
        @DisplayName("aiSummaryGeneratedAt > 60 мин назад → cooldown не применяется")
        void oldGeneratedAt_noCooldown() {
            Session session = sessionWithFullContent();
            session.setAiSummaryGeneratedAt(OffsetDateTime.now().minusMinutes(90));
            UUID practitionerId = UUID.randomUUID();
            when(sessionRepository.findByIdAndPractitionerId(eq(session.getId()), eq(practitionerId)))
                    .thenReturn(Optional.of(session));

            service.triggerSessionSummary(session.getId(), practitionerId);
            // Не должно бросить TooManyRequestsException
        }
    }

    // ── triggerClientSummary ───────────────────────────────────────────────────

    @Nested
    @DisplayName("triggerClientSummary")
    class TriggerClientSummary {

        @Test
        @DisplayName("< 2 сессий с саммари → BusinessException")
        void insufficientSessions_throwsBusinessException() {
            UUID clientId = UUID.randomUUID();
            UUID practitionerId = UUID.randomUUID();
            Client client = Client.builder().id(clientId).build();
            client.setAiOverallSummaryGeneratedAt(null);

            when(clientRepository.findByIdAndPractitionerId(clientId, practitionerId))
                    .thenReturn(Optional.of(client));
            when(sessionRepository.findTop10CompletedWithSummaryByClientId(clientId))
                    .thenReturn(Collections.singletonList(sessionWithFullContent()));

            assertThatThrownBy(() -> service.triggerClientSummary(clientId, practitionerId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Недостаточно сессий");
        }

        @Test
        @DisplayName("0 сессий → BusinessException")
        void zeroSessions_throwsBusinessException() {
            UUID clientId = UUID.randomUUID();
            UUID practitionerId = UUID.randomUUID();
            Client client = Client.builder().id(clientId).build();

            when(clientRepository.findByIdAndPractitionerId(clientId, practitionerId))
                    .thenReturn(Optional.of(client));
            when(sessionRepository.findTop10CompletedWithSummaryByClientId(clientId))
                    .thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> service.triggerClientSummary(clientId, practitionerId))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("cooldown активен → TooManyRequestsException")
        void cooldownActive_throwsTooManyRequests() {
            UUID clientId = UUID.randomUUID();
            UUID practitionerId = UUID.randomUUID();
            Client client = Client.builder().id(clientId).build();
            client.setAiOverallSummaryGeneratedAt(OffsetDateTime.now().minusMinutes(10));

            when(clientRepository.findByIdAndPractitionerId(clientId, practitionerId))
                    .thenReturn(Optional.of(client));

            assertThatThrownBy(() -> service.triggerClientSummary(clientId, practitionerId))
                    .isInstanceOf(TooManyRequestsException.class);
        }

        @Test
        @DisplayName("2+ сессий, нет cooldown → генерация запускается (generateClientSummaryAsync)")
        void enoughSessions_generationTriggered() {
            UUID clientId = UUID.randomUUID();
            UUID practitionerId = UUID.randomUUID();
            Client client = Client.builder().id(clientId).build();

            when(clientRepository.findByIdAndPractitionerId(clientId, practitionerId))
                    .thenReturn(Optional.of(client));
            when(sessionRepository.findTop10CompletedWithSummaryByClientId(clientId))
                    .thenReturn(List.of(sessionWithSummary(), sessionWithSummary()));

            // generateClientSummaryAsync is @Async so it won't run in test — just check no exception
            service.triggerClientSummary(clientId, practitionerId);
        }
    }

    // ── generateClientSummaryAsync (direct call) ──────────────────────────────

    @Nested
    @DisplayName("generateClientSummaryAsync (direct)")
    class GenerateClientSummaryAsync {

        @Test
        @DisplayName("2 сессии с саммари → провайдер вызывается, клиент обновляется")
        void enoughSessions_clientSummaryIsSaved() {
            UUID clientId = UUID.randomUUID();
            Client client = Client.builder().id(clientId).practitioner(
                    Practitioner.builder().id(UUID.randomUUID()).build()).build();

            Session s1 = sessionWithSummary();
            Session s2 = sessionWithSummary();

            when(sessionRepository.findTop10CompletedWithSummaryByClientId(clientId))
                    .thenReturn(List.of(s1, s2));
            when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(aiProvider.generateSummary(anyString())).thenReturn("Динамика клиента");

            service.generateClientSummaryAsync(clientId);

            ArgumentCaptor<Client> captor = ArgumentCaptor.forClass(Client.class);
            verify(clientRepository).save(captor.capture());
            assertThat(captor.getValue().getAiOverallSummary()).isEqualTo("Динамика клиента");
            assertThat(captor.getValue().getAiOverallSummaryGeneratedAt()).isNotNull();
        }

        @Test
        @DisplayName("AiProviderException → клиент не сохраняется")
        void providerException_clientNotSaved() {
            UUID clientId = UUID.randomUUID();

            when(sessionRepository.findTop10CompletedWithSummaryByClientId(clientId))
                    .thenReturn(List.of(sessionWithSummary(), sessionWithSummary()));
            when(aiProvider.generateSummary(anyString())).thenThrow(new AiProviderException("Ошибка"));

            service.generateClientSummaryAsync(clientId);

            verify(clientRepository, never()).save(any());
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Session sessionWithShortContent() {
        Session s = Session.builder()
                .id(UUID.randomUUID())
                .preSessionRequest("Краткий")
                .build();
        return s;
    }

    private Session sessionWithFullContent() {
        Session s = Session.builder()
                .id(UUID.randomUUID())
                .preSessionRequest("Страхи одиночества и тревога при общении с людьми")
                .regressionTarget(Session.RegressionTarget.PAST_LIFE)
                .regressionPeriod("Средневековье")
                .regressionSetting("Тёмный лес, холодно, одиноко")
                .keyScenes("Сцена одиночества в лесу, смерть в изоляции")
                .keyEmotions(new String[]{"Страх", "Одиночество", "Печаль"})
                .keyInsights("Понял, что страх одиночества идёт из прошлой жизни")
                .blocksReleased("Страх быть отвергнутым")
                .postSessionState("Лёгкость, спокойствие")
                .nextSessionPlan("Проработать отношения с матерью")
                .scheduledAt(OffsetDateTime.now().minusDays(7))
                .build();
        return s;
    }

    private Session sessionWithSummary() {
        Session s = sessionWithFullContent();
        s.setAiSummary("Саммари сессии — клиент проработал страхи прошлой жизни");
        s.setAiSummaryGeneratedAt(OffsetDateTime.now().minusHours(2));
        return s;
    }
}
