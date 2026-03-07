package com.vogulev.regreso.controller;

import com.vogulev.regreso.dto.response.ScheduleResponse;
import com.vogulev.regreso.dto.response.ScheduleSessionItem;
import com.vogulev.regreso.entity.Session;
import com.vogulev.regreso.repository.SessionRepository;
import com.vogulev.regreso.security.PractitionerDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final SessionRepository sessionRepository;

    @GetMapping("/today")
    @Transactional(readOnly = true)
    public ResponseEntity<ScheduleResponse> getToday(
            @AuthenticationPrincipal PractitionerDetails user) {
        ZoneId zone = resolveZone(user.getPractitioner().getTimezone());
        OffsetDateTime from = LocalDate.now(zone).atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime to = from.plusDays(1).minusNanos(1);
        return ResponseEntity.ok(buildResponse(user.getId(), from, to));
    }

    @GetMapping("/week")
    @Transactional(readOnly = true)
    public ResponseEntity<ScheduleResponse> getWeek(
            @AuthenticationPrincipal PractitionerDetails user) {
        ZoneId zone = resolveZone(user.getPractitioner().getTimezone());
        OffsetDateTime from = LocalDate.now(zone).atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime to = from.plusDays(7).minusNanos(1);
        return ResponseEntity.ok(buildResponse(user.getId(), from, to));
    }

    @GetMapping("/month")
    @Transactional(readOnly = true)
    public ResponseEntity<ScheduleResponse> getMonth(
            @AuthenticationPrincipal PractitionerDetails user) {
        ZoneId zone = resolveZone(user.getPractitioner().getTimezone());
        OffsetDateTime from = LocalDate.now(zone).atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime to = from.plusDays(30).minusNanos(1);
        return ResponseEntity.ok(buildResponse(user.getId(), from, to));
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private ScheduleResponse buildResponse(UUID practitionerId,
                                           OffsetDateTime from,
                                           OffsetDateTime to) {
        List<Session> sessions = sessionRepository
                .findByPractitionerIdAndScheduledAtBetweenOrderByScheduledAt(
                        practitionerId, from, to);

        List<ScheduleSessionItem> items = sessions.stream()
                .map(this::toItem)
                .collect(Collectors.toList());

        int completedCount = (int) sessions.stream()
                .filter(s -> s.getStatus() == Session.Status.COMPLETED)
                .count();
        int upcomingCount = (int) sessions.stream()
                .filter(s -> s.getStatus() == Session.Status.SCHEDULED)
                .count();

        return ScheduleResponse.builder()
                .sessions(items)
                .totalCount(items.size())
                .completedCount(completedCount)
                .upcomingCount(upcomingCount)
                .build();
    }

    private ScheduleSessionItem toItem(Session s) {
        return ScheduleSessionItem.builder()
                .id(s.getId())
                .clientId(s.getClient().getId())
                .clientFullName(s.getClient().getFullName())
                .clientHasContraindications(Boolean.TRUE.equals(s.getClient().getHasContraindications()))
                .sessionNumber(s.getSessionNumber() != null ? s.getSessionNumber() : 0)
                .scheduledAt(s.getScheduledAt())
                .durationMin(s.getDurationMin() != null ? s.getDurationMin() : 120)
                .status(s.getStatus() != null ? s.getStatus().name() : null)
                .preSessionRequest(s.getPreSessionRequest())
                .isPaid(s.getIsPaid())
                .telegramReminderSent(Boolean.TRUE.equals(s.getReminder24hSent()))
                .build();
    }

    private ZoneId resolveZone(String timezone) {
        if (timezone == null || timezone.isBlank()) return ZoneId.of("Europe/Moscow");
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            return ZoneId.of("Europe/Moscow");
        }
    }
}
