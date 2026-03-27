package com.vogulev.regreso.service.impl;

import com.vogulev.regreso.ai.AiSummaryService;
import com.vogulev.regreso.dto.request.CancelSessionRequest;
import com.vogulev.regreso.dto.request.CreateSessionRequest;
import com.vogulev.regreso.dto.request.UpdateSessionRequest;
import com.vogulev.regreso.dto.response.*;
import com.vogulev.regreso.entity.*;
import com.vogulev.regreso.event.SessionCompletedEvent;
import com.vogulev.regreso.exception.BusinessException;
import com.vogulev.regreso.exception.ResourceNotFoundException;
import com.vogulev.regreso.mapper.SessionMapper;
import com.vogulev.regreso.repository.*;
import com.vogulev.regreso.service.FileStorageService;
import com.vogulev.regreso.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;
    private final ClientRepository clientRepository;
    private final PractitionerRepository practitionerRepository;
    private final ReminderRepository reminderRepository;
    private final ClientThemeRepository clientThemeRepository;
    private final HomeworkRepository homeworkRepository;
    private final SessionMediaRepository sessionMediaRepository;
    private final SessionMapper sessionMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final AiSummaryService aiSummaryService;
    private final FileStorageService fileStorageService;

    @Override
    public SessionResponse createSession(CreateSessionRequest request, UUID practitionerId) {
        Practitioner practitioner = practitionerRepository.findById(practitionerId)
                .orElseThrow(() -> new ResourceNotFoundException("Практик не найден"));

        Client client = clientRepository.findByIdAndPractitionerId(request.getClientId(), practitionerId)
                .orElseThrow(() -> new ResourceNotFoundException("Клиент не найден"));

        int sessionNumber = sessionRepository.getNextSessionNumber(request.getClientId());
        int durationMin = request.getDurationMin() != null
                ? request.getDurationMin()
                : (practitioner.getDefaultSessionDurationMin() != null
                        ? practitioner.getDefaultSessionDurationMin()
                        : 120);

        Session session = Session.builder()
                .practitioner(practitioner)
                .client(client)
                .scheduledAt(request.getScheduledAt())
                .durationMin(durationMin)
                .sessionNumber(sessionNumber)
                .preSessionRequest(request.getPreSessionRequest())
                .price(request.getPrice())
                .build();

        session = sessionRepository.save(session);
        createReminders(session);

        return enrichWithMedia(sessionMapper.toResponse(session), session.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public SessionResponse getSession(UUID sessionId, UUID practitionerId) {
        Session session = findSessionOrThrow(sessionId, practitionerId);
        return enrichWithMedia(sessionMapper.toResponse(session), session.getId());
    }

    @Override
    public SessionResponse updateSession(UUID sessionId, UpdateSessionRequest request, UUID practitionerId) {
        Session session = findSessionOrThrow(sessionId, practitionerId);
        applyUpdate(session, request);
        return enrichWithMedia(sessionMapper.toResponse(session), session.getId());
    }

    @Override
    public SessionResponse completeSession(UUID sessionId, UpdateSessionRequest request, UUID practitionerId) {
        Session session = findSessionOrThrow(sessionId, practitionerId);

        if (session.getStatus() == Session.Status.CANCELLED) {
            throw new BusinessException("Нельзя завершить отменённую сессию");
        }
        if (session.getStatus() == Session.Status.COMPLETED) {
            throw new BusinessException("Сессия уже завершена");
        }

        if (request != null) {
            applyUpdate(session, request);
        }
        session.setStatus(Session.Status.COMPLETED);

        SessionResponse response = enrichWithMedia(sessionMapper.toResponse(session), session.getId());

        // Публикуем событие для будущей AI-генерации саммари (асинхронно)
        eventPublisher.publishEvent(new SessionCompletedEvent(this, session.getId()));

        return response;
    }

    @Override
    public SessionResponse cancelSession(UUID sessionId, CancelSessionRequest request, UUID practitionerId) {
        Session session = findSessionOrThrow(sessionId, practitionerId);

        if (session.getStatus() == Session.Status.COMPLETED) {
            throw new BusinessException("Нельзя отменить завершённую сессию");
        }
        if (session.getStatus() == Session.Status.CANCELLED) {
            throw new BusinessException("Сессия уже отменена");
        }

        session.setStatus(Session.Status.CANCELLED);
        if (request != null && request.getReason() != null) {
            session.setPractitionerNotes(request.getReason());
        }

        return enrichWithMedia(sessionMapper.toResponse(session), session.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public SessionPrepResponse getSessionPrep(UUID sessionId, UUID practitionerId) {
        Session session = findSessionOrThrow(sessionId, practitionerId);
        Client client = session.getClient();

        // Последние 3 сессии клиента (кроме текущей)
        List<SessionSummaryResponse> recentSessions = sessionRepository
                .findTop3ByClientIdOrderByScheduledAtDesc(client.getId())
                .stream()
                .filter(s -> !s.getId().equals(sessionId))
                .limit(3)
                .map(sessionMapper::toSummaryResponse)
                .collect(Collectors.toList());

        // Активные темы
        List<ClientThemeShortResponse> activeThemes = clientThemeRepository
                .findActiveByClientId(client.getId())
                .stream()
                .map(theme -> ClientThemeShortResponse.builder()
                        .id(theme.getId())
                        .title(theme.getTitle())
                        .isResolved(Boolean.TRUE.equals(theme.getIsResolved()))
                        .sessionsCount(clientThemeRepository.countSessionsByThemeId(theme.getId()))
                        .build())
                .collect(Collectors.toList());

        // Последнее задание
        HomeworkShortResponse lastHomework = homeworkRepository
                .findFirstByClientIdOrderByCreatedAtDesc(client.getId())
                .map(hw -> HomeworkShortResponse.builder()
                        .id(hw.getId())
                        .title(hw.getTitle())
                        .status(hw.getStatus() != null ? hw.getStatus().name() : null)
                        .dueDate(hw.getDueDate())
                        .clientResponse(hw.getClientResponse())
                        .build())
                .orElse(null);

        // План из предыдущей сессии
        String nextSessionPlan = sessionRepository
                .findByClientIdOrderByScheduledAtDesc(client.getId())
                .stream()
                .filter(s -> !s.getId().equals(sessionId))
                .filter(s -> s.getNextSessionPlan() != null)
                .findFirst()
                .map(Session::getNextSessionPlan)
                .orElse(null);

        return SessionPrepResponse.builder()
                .sessionId(session.getId())
                .scheduledAt(session.getScheduledAt())
                .sessionNumber(session.getSessionNumber() != null ? session.getSessionNumber() : 0)
                .clientId(client.getId())
                .clientFullName(client.getFullName())
                .clientBirthDate(client.getBirthDate())
                .hasContraindications(Boolean.TRUE.equals(client.getHasContraindications()))
                .contraindicationsNotes(client.getContraindicationsNotes())
                .initialRequest(client.getInitialRequest())
                .presentingIssues(client.getPresentingIssues() != null
                        ? Arrays.asList(client.getPresentingIssues()) : Collections.emptyList())
                .recentSessions(recentSessions)
                .activeThemes(activeThemes)
                .lastHomework(lastHomework)
                .nextSessionPlan(nextSessionPlan)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SessionSummaryStatusResponse getSessionSummary(UUID sessionId, UUID practitionerId) {
        Session session = findSessionOrThrow(sessionId, practitionerId);

        boolean ready = session.getAiSummary() != null;
        return SessionSummaryStatusResponse.builder()
                .sessionId(session.getId())
                .status(ready ? "READY" : "PENDING")
                .summary(session.getAiSummary())
                .generatedAt(session.getAiSummaryGeneratedAt())
                .build();
    }

    @Override
    public void triggerSessionSummaryGeneration(UUID sessionId, UUID practitionerId) {
        aiSummaryService.triggerSessionSummary(sessionId, practitionerId);
    }

    @Override
    public SessionMediaResponse uploadSessionPhoto(UUID sessionId, MultipartFile file, String caption, UUID practitionerId)
            throws IOException {
        Session session = findSessionOrThrow(sessionId, practitionerId);
        validateSessionPhoto(file);

        String fileUrl = fileStorageService.store(file, "session-photos");
        SessionMedia media = SessionMedia.builder()
                .session(session)
                .mediaType("PHOTO")
                .fileKey(fileUrl)
                .fileName(resolveFileName(file))
                .mimeType(resolveMimeType(file))
                .fileSizeBytes(Math.toIntExact(file.getSize()))
                .caption(normalizeCaption(caption))
                .build();

        return toMediaResponse(sessionMediaRepository.save(media));
    }

    @Override
    public void deleteSessionPhoto(UUID sessionId, UUID mediaId, UUID practitionerId) {
        findSessionOrThrow(sessionId, practitionerId);

        SessionMedia media = sessionMediaRepository.findByIdAndSessionId(mediaId, sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Фото протокола не найдено"));

        sessionMediaRepository.delete(media);
        fileStorageService.delete(media.getFileKey());
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private Session findSessionOrThrow(UUID sessionId, UUID practitionerId) {
        return sessionRepository.findByIdAndPractitionerId(sessionId, practitionerId)
                .orElseThrow(() -> new ResourceNotFoundException("Сессия не найдена"));
    }

    private SessionResponse enrichWithMedia(SessionResponse response, UUID sessionId) {
        response.setMedia(getMediaResponses(sessionId));
        return response;
    }

    private List<SessionMediaResponse> getMediaResponses(UUID sessionId) {
        return sessionMediaRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(this::toMediaResponse)
                .toList();
    }

    private SessionMediaResponse toMediaResponse(SessionMedia media) {
        return SessionMediaResponse.builder()
                .id(media.getId())
                .mediaType(media.getMediaType())
                .fileUrl(media.getFileKey())
                .fileName(media.getFileName())
                .mimeType(media.getMimeType())
                .fileSizeBytes(media.getFileSizeBytes())
                .durationSec(media.getDurationSec())
                .caption(media.getCaption())
                .createdAt(media.getCreatedAt())
                .build();
    }

    private void validateSessionPhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Файл изображения обязателен");
        }

        String mimeType = resolveMimeType(file);
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase(Locale.ROOT) : "";
        boolean supportedByMime = mimeType.startsWith("image/");
        boolean supportedByExtension = filename.endsWith(".jpg")
                || filename.endsWith(".jpeg")
                || filename.endsWith(".png")
                || filename.endsWith(".webp")
                || filename.endsWith(".heic")
                || filename.endsWith(".heif");

        if (!supportedByMime && !supportedByExtension) {
            throw new BusinessException("Можно загружать только изображения протокола");
        }
    }

    private String resolveMimeType(MultipartFile file) {
        return file.getContentType() != null ? file.getContentType() : "application/octet-stream";
    }

    private String resolveFileName(MultipartFile file) {
        return file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank()
                ? file.getOriginalFilename()
                : "session-photo";
    }

    private String normalizeCaption(String caption) {
        if (caption == null) {
            return null;
        }
        String trimmed = caption.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void applyUpdate(Session session, UpdateSessionRequest req) {
        if (req.getScheduledAt() != null)       session.setScheduledAt(req.getScheduledAt());
        if (req.getDurationMin() != null)        session.setDurationMin(req.getDurationMin());

        // [1]
        if (req.getPreSessionRequest() != null)  session.setPreSessionRequest(req.getPreSessionRequest());
        if (req.getPreSessionState() != null)    session.setPreSessionState(req.getPreSessionState());
        if (req.getPreSessionScore() != null)    session.setPreSessionScore(req.getPreSessionScore());

        // [2]
        if (req.getInductionMethod() != null)    session.setInductionMethod(req.getInductionMethod());
        if (req.getInductionNotes() != null)     session.setInductionNotes(req.getInductionNotes());
        if (req.getTranceDepth() != null) {
            session.setTranceDepth(Session.TranceDepth.valueOf(req.getTranceDepth()));
        }

        // [3]
        if (req.getRegressionPeriod() != null)   session.setRegressionPeriod(req.getRegressionPeriod());
        if (req.getRegressionSetting() != null)  session.setRegressionSetting(req.getRegressionSetting());
        if (req.getRegressionTarget() != null) {
            session.setRegressionTarget(Session.RegressionTarget.valueOf(req.getRegressionTarget()));
        }

        // [4]
        if (req.getKeyScenes() != null)          session.setKeyScenes(req.getKeyScenes());
        if (req.getKeyEmotions() != null)        session.setKeyEmotions(req.getKeyEmotions().toArray(String[]::new));
        if (req.getKeyInsights() != null)        session.setKeyInsights(req.getKeyInsights());
        if (req.getSymbolicImages() != null)     session.setSymbolicImages(req.getSymbolicImages());

        // [5]
        if (req.getBlocksReleased() != null)     session.setBlocksReleased(req.getBlocksReleased());
        if (req.getHealingOccurred() != null)    session.setHealingOccurred(req.getHealingOccurred());
        if (req.getHealingNotes() != null)       session.setHealingNotes(req.getHealingNotes());

        // [6]
        if (req.getPostSessionState() != null)   session.setPostSessionState(req.getPostSessionState());
        if (req.getPostSessionScore() != null)   session.setPostSessionScore(req.getPostSessionScore());
        if (req.getIntegrationNotes() != null)   session.setIntegrationNotes(req.getIntegrationNotes());

        // [7]
        if (req.getPractitionerNotes() != null)  session.setPractitionerNotes(req.getPractitionerNotes());
        if (req.getNextSessionPlan() != null)    session.setNextSessionPlan(req.getNextSessionPlan());

        // Финансы
        if (req.getPrice() != null)              session.setPrice(req.getPrice());
        if (req.getIsPaid() != null)             session.setIsPaid(req.getIsPaid());
    }

    private void createReminders(Session session) {
        Reminder reminder24h = Reminder.builder()
                .session(session)
                .recipientType(Reminder.RecipientType.CLIENT)
                .channel(Reminder.Channel.TELEGRAM)
                .sendAt(session.getScheduledAt().minusHours(24))
                .build();

        Reminder reminder1h = Reminder.builder()
                .session(session)
                .recipientType(Reminder.RecipientType.CLIENT)
                .channel(Reminder.Channel.TELEGRAM)
                .sendAt(session.getScheduledAt().minusHours(1))
                .build();

        reminderRepository.save(reminder24h);
        reminderRepository.save(reminder1h);
    }
}
