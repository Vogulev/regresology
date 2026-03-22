package com.vogulev.regreso.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vogulev.regreso.dto.request.PublicBookingRequest;
import com.vogulev.regreso.dto.response.*;
import com.vogulev.regreso.entity.BookingSettings;
import com.vogulev.regreso.entity.Client;
import com.vogulev.regreso.entity.Practitioner;
import com.vogulev.regreso.entity.Session;
import com.vogulev.regreso.exception.ResourceNotFoundException;
import com.vogulev.regreso.repository.BookingSettingsRepository;
import com.vogulev.regreso.repository.CertificateRepository;
import com.vogulev.regreso.repository.ClientRepository;
import com.vogulev.regreso.repository.SessionRepository;
import com.vogulev.regreso.service.PublicBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PublicBookingServiceImpl implements PublicBookingService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int DAY_START_HOUR = 9;
    private static final int DAY_END_HOUR = 20;
    private static final BookingAvailabilityMode DEFAULT_AVAILABILITY_MODE = BookingAvailabilityMode.DEFAULT;

    private final BookingSettingsRepository bookingSettingsRepository;
    private final SessionRepository sessionRepository;
    private final ClientRepository clientRepository;
    private final CertificateRepository certificateRepository;

    @Override
    @Transactional(readOnly = true)
    public PublicBookingPageResponse getBookingPage(String slug) {
        BookingSettings settings = findSettings(slug);
        Practitioner p = settings.getPractitioner();

        List<BookingServiceItemDto> services = parseServices(settings.getServices());

        List<CertificateResponse> certificates = certificateRepository
                .findByPractitionerIdOrderByCreatedAtDesc(p.getId())
                .stream()
                .map(c -> CertificateResponse.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .fileUrl(c.getFileUrl())
                        .originalFilename(c.getOriginalFilename())
                        .createdAt(c.getCreatedAt())
                        .build())
                .toList();

        return PublicBookingPageResponse.builder()
                .practitionerName(p.getFirstName() + (p.getLastName() != null ? " " + p.getLastName() : ""))
                .practitionerBio(p.getBio())
                .practitionerPhotoUrl(p.getPhotoUrl())
                .welcomeMessage(settings.getWelcomeMessage())
                .services(services)
                .certificates(certificates)
                .requireIntakeForm(Boolean.TRUE.equals(settings.getRequireIntakeForm()))
                .advanceDays(settings.getAdvanceDays())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AvailableSlotsResponse getAvailableSlots(String slug, String date) {
        BookingSettings settings = findSettings(slug);
        Practitioner practitioner = settings.getPractitioner();

        ZoneId zone = resolveZone(practitioner.getTimezone());
        LocalDate localDate = LocalDate.parse(date);

        LocalDate today = LocalDate.now(zone);
        int advanceDays = settings.getAdvanceDays() != null ? settings.getAdvanceDays() : 30;
        if (localDate.isBefore(today) || localDate.isAfter(today.plusDays(advanceDays))) {
            return new AvailableSlotsResponse(Collections.emptyList());
        }

        int durationMin = settings.getDefaultDurationMin() != null ? settings.getDefaultDurationMin() : 120;
        int bufferMin = settings.getBufferMin() != null ? settings.getBufferMin() : 0;

        OffsetDateTime dayStart = localDate.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime dayEnd = dayStart.plusDays(1).minusNanos(1);
        List<Session> existing = sessionRepository
                .findByPractitionerIdAndScheduledAtBetweenOrderByScheduledAt(
                        practitioner.getId(), dayStart, dayEnd);

        OffsetDateTime now = OffsetDateTime.now(zone);
        BookingAvailabilityMode availabilityMode = resolveAvailabilityMode(settings.getAvailabilityMode());
        if (availabilityMode == BookingAvailabilityMode.CUSTOM) {
            return buildCustomSlots(settings, localDate, zone, now, existing, durationMin, bufferMin);
        }

        int slotInterval = durationMin + bufferMin;
        OffsetDateTime cursor = localDate.atTime(DAY_START_HOUR, 0).atZone(zone).toOffsetDateTime();
        OffsetDateTime cutoff = localDate.atTime(DAY_END_HOUR, 0).atZone(zone).toOffsetDateTime();

        return new AvailableSlotsResponse(generateSlots(cursor, cutoff, slotInterval, durationMin, bufferMin, now, existing, Collections.emptyList(), localDate, zone));
    }

    @Override
    public PublicBookingConfirmation createBooking(String slug, PublicBookingRequest request) {
        BookingSettings settings = findSettings(slug);
        Practitioner practitioner = settings.getPractitioner();
        String normalizedTelegram = normalizeTelegramUsername(request.getTelegramUsername());

        // Find or create client by phone
        Client client = clientRepository
                .findByPractitionerIdAndPhoneAndIsArchivedFalse(practitioner.getId(), request.getPhone())
                .orElseGet(() -> clientRepository.save(Client.builder()
                        .practitioner(practitioner)
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .phone(request.getPhone())
                        .telegramUsername(normalizedTelegram)
                        .initialRequest(request.getClientRequest())
                        .build()));

        if (client.getTelegramUsername() == null && normalizedTelegram != null) {
            client.setTelegramUsername(normalizedTelegram);
            clientRepository.save(client);
        }

        OffsetDateTime scheduledAt = OffsetDateTime.parse(request.getSelectedSlot(),
                DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        Integer durationMin = settings.getDefaultDurationMin() != null
                ? settings.getDefaultDurationMin() : 120;

        Integer nextNumber = sessionRepository.getNextSessionNumber(client.getId());

        sessionRepository.save(Session.builder()
                .practitioner(practitioner)
                .client(client)
                .scheduledAt(scheduledAt)
                .durationMin(durationMin)
                .preSessionRequest(request.getClientRequest())
                .sessionNumber(nextNumber)
                .build());

        String name = practitioner.getFirstName()
                + (practitioner.getLastName() != null ? " " + practitioner.getLastName() : "");

        return PublicBookingConfirmation.builder()
                .message("Вы успешно записаны на сессию. Практик свяжется с вами для подтверждения.")
                .sessionAt(scheduledAt)
                .practitionerName(name)
                .build();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private BookingSettings findSettings(String slug) {
        return bookingSettingsRepository.findBySlugAndIsEnabledTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Страница записи не найдена"));
    }

    private List<BookingServiceItemDto> parseServices(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<BookingDayAvailabilityDto> parseWeeklyAvailability(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private BookingAvailabilityMode resolveAvailabilityMode(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_AVAILABILITY_MODE;
        }
        try {
            return BookingAvailabilityMode.valueOf(value);
        } catch (IllegalArgumentException e) {
            return DEFAULT_AVAILABILITY_MODE;
        }
    }

    private AvailableSlotsResponse buildCustomSlots(
            BookingSettings settings,
            LocalDate localDate,
            ZoneId zone,
            OffsetDateTime now,
            List<Session> existing,
            int durationMin,
            int bufferMin
    ) {
        List<BookingDayAvailabilityDto> weeklyAvailability = parseWeeklyAvailability(settings.getWeeklyAvailability());
        BookingDayAvailabilityDto dayAvailability = weeklyAvailability.stream()
                .filter(item -> item.getDayOfWeek() != null && item.getDayOfWeek() == localDate.getDayOfWeek().getValue())
                .findFirst()
                .orElse(null);

        if (dayAvailability == null || !Boolean.TRUE.equals(dayAvailability.getIsWorkingDay())) {
            return new AvailableSlotsResponse(Collections.emptyList());
        }
        if (dayAvailability.getStartTime() == null || dayAvailability.getEndTime() == null) {
            return new AvailableSlotsResponse(Collections.emptyList());
        }

        try {
            LocalTime startTime = LocalTime.parse(dayAvailability.getStartTime());
            LocalTime endTime = LocalTime.parse(dayAvailability.getEndTime());
            if (!endTime.isAfter(startTime)) {
                return new AvailableSlotsResponse(Collections.emptyList());
            }

            int slotInterval = dayAvailability.getSlotIntervalMin() != null
                    ? dayAvailability.getSlotIntervalMin()
                    : durationMin + bufferMin;
            if (slotInterval <= 0) {
                return new AvailableSlotsResponse(Collections.emptyList());
            }

            OffsetDateTime cursor = localDate.atTime(startTime).atZone(zone).toOffsetDateTime();
            OffsetDateTime cutoff = localDate.atTime(endTime).atZone(zone).toOffsetDateTime();
            List<BookingBreakItemDto> breaks = dayAvailability.getBreaks() != null ? dayAvailability.getBreaks() : Collections.emptyList();

            return new AvailableSlotsResponse(generateSlots(cursor, cutoff, slotInterval, durationMin, bufferMin, now, existing, breaks, localDate, zone));
        } catch (DateTimeException e) {
            return new AvailableSlotsResponse(Collections.emptyList());
        }
    }

    private List<AvailableSlotDto> generateSlots(
            OffsetDateTime cursor,
            OffsetDateTime cutoff,
            int slotInterval,
            int durationMin,
            int bufferMin,
            OffsetDateTime now,
            List<Session> existing,
            List<BookingBreakItemDto> breaks,
            LocalDate localDate,
            ZoneId zone
    ) {
        List<AvailableSlotDto> slots = new ArrayList<>();
        while (!cursor.plusMinutes(durationMin).isAfter(cutoff)) {
            OffsetDateTime slotEnd = cursor.plusMinutes(durationMin);
            if (cursor.isAfter(now)
                    && !overlapsExisting(cursor, slotEnd, existing, durationMin, bufferMin)
                    && !overlapsBreaks(cursor, slotEnd, breaks, localDate, zone)) {
                slots.add(new AvailableSlotDto(cursor, slotEnd));
            }
            cursor = cursor.plusMinutes(slotInterval);
        }
        return slots;
    }

    private boolean overlapsBreaks(
            OffsetDateTime start,
            OffsetDateTime end,
            List<BookingBreakItemDto> breaks,
            LocalDate localDate,
            ZoneId zone
    ) {
        for (BookingBreakItemDto item : breaks) {
            if (item.getStartTime() == null || item.getEndTime() == null) {
                continue;
            }
            try {
                OffsetDateTime breakStart = localDate.atTime(LocalTime.parse(item.getStartTime())).atZone(zone).toOffsetDateTime();
                OffsetDateTime breakEnd = localDate.atTime(LocalTime.parse(item.getEndTime())).atZone(zone).toOffsetDateTime();
                if (start.isBefore(breakEnd) && end.isAfter(breakStart)) {
                    return true;
                }
            } catch (DateTimeException ignored) {
                return true;
            }
        }
        return false;
    }

    private ZoneId resolveZone(String timezone) {
        if (timezone == null || timezone.isBlank()) return ZoneId.of("Europe/Moscow");
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            return ZoneId.of("Europe/Moscow");
        }
    }

    private boolean overlapsExisting(
            OffsetDateTime start,
            OffsetDateTime end,
            List<Session> sessions,
            int durationMin,
            int bufferMin
    ) {
        OffsetDateTime candidateBlockedUntil = end.plusMinutes(Math.max(bufferMin, 0));
        for (Session s : sessions) {
            if (s.getStatus() == Session.Status.CANCELLED || s.getStatus() == Session.Status.NO_SHOW) {
                continue;
            }
            OffsetDateTime sStart = s.getScheduledAt();
            OffsetDateTime sEnd = sStart.plusMinutes(s.getDurationMin() != null ? s.getDurationMin() : durationMin);
            OffsetDateTime existingBlockedUntil = sEnd.plusMinutes(Math.max(bufferMin, 0));
            if (start.isBefore(existingBlockedUntil) && candidateBlockedUntil.isAfter(sStart)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeTelegramUsername(String telegramUsername) {
        if (telegramUsername == null) return null;
        String normalized = telegramUsername.trim();
        if (normalized.isBlank()) return null;
        return normalized.startsWith("@") ? normalized : "@" + normalized;
    }
}
