package com.vogulev.regreso.service.impl;

import com.vogulev.regreso.dto.request.CreateThemeRequest;
import com.vogulev.regreso.dto.request.LinkSessionToThemeRequest;
import com.vogulev.regreso.dto.response.ClientThemeResponse;
import com.vogulev.regreso.dto.response.SessionSummaryShort;
import com.vogulev.regreso.entity.*;
import com.vogulev.regreso.exception.BusinessException;
import com.vogulev.regreso.exception.ResourceNotFoundException;
import com.vogulev.regreso.repository.*;
import com.vogulev.regreso.service.ClientThemeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientThemeServiceImpl implements ClientThemeService {

    private final ClientThemeRepository clientThemeRepository;
    private final ThemeSessionRepository themeSessionRepository;
    private final ClientRepository clientRepository;
    private final SessionRepository sessionRepository;
    private final PractitionerRepository practitionerRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ClientThemeResponse> getClientThemes(UUID clientId, UUID practitionerId) {
        checkClientAccess(clientId, practitionerId);
        return clientThemeRepository
                .findByClientIdAndPractitionerIdOrderByCreatedAtDesc(clientId, practitionerId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ClientThemeResponse createTheme(UUID clientId, CreateThemeRequest request, UUID practitionerId) {
        Client client = clientRepository.findByIdAndPractitionerId(clientId, practitionerId)
                .orElseThrow(() -> new ResourceNotFoundException("Клиент не найден"));
        Practitioner practitioner = practitionerRepository.findById(practitionerId)
                .orElseThrow(() -> new ResourceNotFoundException("Практик не найден"));

        ClientTheme theme = ClientTheme.builder()
                .client(client)
                .practitioner(practitioner)
                .title(request.getTitle())
                .description(request.getDescription())
                .firstSeenAt(request.getFirstSeenAt() != null ? request.getFirstSeenAt() : OffsetDateTime.now())
                .build();

        return toResponse(clientThemeRepository.save(theme));
    }

    @Override
    public ClientThemeResponse linkSession(UUID themeId, LinkSessionToThemeRequest request, UUID practitionerId) {
        ClientTheme theme = clientThemeRepository.findByIdAndPractitionerId(themeId, practitionerId)
                .orElseThrow(() -> new ResourceNotFoundException("Тема не найдена"));

        Session session = sessionRepository.findByIdAndPractitionerId(request.getSessionId(), practitionerId)
                .orElseThrow(() -> new ResourceNotFoundException("Сессия не найдена"));

        ThemeSessionId tsId = new ThemeSessionId(theme.getId(), session.getId());
        if (themeSessionRepository.existsById(tsId)) {
            throw new BusinessException("Сессия уже привязана к этой теме");
        }

        ThemeSession ts = ThemeSession.builder()
                .id(tsId)
                .theme(theme)
                .session(session)
                .notes(request.getNotes())
                .build();
        themeSessionRepository.save(ts);

        return toResponse(theme);
    }

    @Override
    public ClientThemeResponse resolveTheme(UUID themeId, UUID practitionerId) {
        ClientTheme theme = clientThemeRepository.findByIdAndPractitionerId(themeId, practitionerId)
                .orElseThrow(() -> new ResourceNotFoundException("Тема не найдена"));

        if (Boolean.TRUE.equals(theme.getIsResolved())) {
            throw new BusinessException("Тема уже помечена как проработанная");
        }

        theme.setIsResolved(true);
        theme.setResolvedAt(OffsetDateTime.now());
        return toResponse(theme);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void checkClientAccess(UUID clientId, UUID practitionerId) {
        clientRepository.findByIdAndPractitionerId(clientId, practitionerId)
                .orElseThrow(() -> new ResourceNotFoundException("Клиент не найден"));
    }

    private ClientThemeResponse toResponse(ClientTheme theme) {
        List<ThemeSession> themeSessions = themeSessionRepository.findByThemeIdWithSession(theme.getId());
        List<SessionSummaryShort> sessions = themeSessions.stream()
                .map(ts -> SessionSummaryShort.builder()
                        .id(ts.getSession().getId())
                        .sessionNumber(ts.getSession().getSessionNumber() != null
                                ? ts.getSession().getSessionNumber() : 0)
                        .scheduledAt(ts.getSession().getScheduledAt())
                        .build())
                .collect(Collectors.toList());

        return ClientThemeResponse.builder()
                .id(theme.getId())
                .title(theme.getTitle())
                .description(theme.getDescription())
                .isResolved(Boolean.TRUE.equals(theme.getIsResolved()))
                .resolvedAt(theme.getResolvedAt())
                .firstSeenAt(theme.getFirstSeenAt())
                .sessionsCount(sessions.size())
                .sessions(sessions)
                .build();
    }
}
