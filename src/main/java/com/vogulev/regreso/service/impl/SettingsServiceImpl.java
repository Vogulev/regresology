package com.vogulev.regreso.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vogulev.regreso.dto.request.BookingSettingsRequest;
import com.vogulev.regreso.dto.request.NotificationSettingsRequest;
import com.vogulev.regreso.dto.request.ProfileSettingsRequest;
import com.vogulev.regreso.dto.response.BookingServiceItemDto;
import com.vogulev.regreso.dto.response.BookingSettingsResponse;
import com.vogulev.regreso.dto.response.NotificationSettingsResponse;
import com.vogulev.regreso.dto.response.ProfileSettingsResponse;
import com.vogulev.regreso.entity.BookingSettings;
import com.vogulev.regreso.entity.Practitioner;
import com.vogulev.regreso.exception.ResourceNotFoundException;
import com.vogulev.regreso.repository.BookingSettingsRepository;
import com.vogulev.regreso.repository.PractitionerRepository;
import com.vogulev.regreso.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SettingsServiceImpl implements SettingsService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final PractitionerRepository practitionerRepository;
    private final BookingSettingsRepository bookingSettingsRepository;

    @Override
    @Transactional(readOnly = true)
    public ProfileSettingsResponse getProfile(UUID practitionerId) {
        Practitioner p = findPractitioner(practitionerId);
        return toProfileResponse(p);
    }

    @Override
    public ProfileSettingsResponse updateProfile(UUID practitionerId, ProfileSettingsRequest request) {
        Practitioner p = findPractitioner(practitionerId);
        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            p.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null) p.setLastName(request.getLastName().isBlank() ? null : request.getLastName().trim());
        if (request.getPhone() != null) p.setPhone(request.getPhone().isBlank() ? null : request.getPhone().trim());
        if (request.getBio() != null) p.setBio(request.getBio().isBlank() ? null : request.getBio().trim());
        if (request.getTimezone() != null && !request.getTimezone().isBlank()) p.setTimezone(request.getTimezone());
        if (request.getDefaultSessionDurationMin() != null) p.setDefaultSessionDurationMin(request.getDefaultSessionDurationMin());
        return toProfileResponse(practitionerRepository.save(p));
    }

    @Override
    @Transactional(readOnly = true)
    public BookingSettingsResponse getBookingSettings(UUID practitionerId) {
        return bookingSettingsRepository.findByPractitionerId(practitionerId)
                .map(this::toBookingResponse)
                .orElseGet(() -> BookingSettingsResponse.builder()
                        .isEnabled(false)
                        .services(Collections.emptyList())
                        .build());
    }

    @Override
    public BookingSettingsResponse updateBookingSettings(UUID practitionerId, BookingSettingsRequest request) {
        Practitioner practitioner = findPractitioner(practitionerId);
        BookingSettings settings = bookingSettingsRepository.findByPractitionerId(practitionerId)
                .orElseGet(() -> BookingSettings.builder().practitioner(practitioner).build());

        if (request.getIsEnabled() != null) settings.setIsEnabled(request.getIsEnabled());
        if (request.getSlug() != null) settings.setSlug(request.getSlug().isBlank() ? null : request.getSlug().trim());
        if (request.getDefaultDurationMin() != null) settings.setDefaultDurationMin(request.getDefaultDurationMin());
        if (request.getBufferMin() != null) settings.setBufferMin(request.getBufferMin());
        if (request.getAdvanceDays() != null) settings.setAdvanceDays(request.getAdvanceDays());
        if (request.getRequireIntakeForm() != null) settings.setRequireIntakeForm(request.getRequireIntakeForm());
        if (request.getWelcomeMessage() != null) settings.setWelcomeMessage(request.getWelcomeMessage().isBlank() ? null : request.getWelcomeMessage());
        if (request.getServices() != null) {
            try {
                settings.setServices(MAPPER.writeValueAsString(request.getServices()));
            } catch (Exception e) {
                settings.setServices(null);
            }
        }

        return toBookingResponse(bookingSettingsRepository.save(settings));
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationSettingsResponse getNotificationSettings(UUID practitionerId) {
        Practitioner p = findPractitioner(practitionerId);
        return NotificationSettingsResponse.builder()
                .sessionRemindersEnabled(Boolean.TRUE.equals(p.getSessionRemindersEnabled()))
                .inactiveClientReminderDays(p.getInactiveClientReminderDays())
                .telegramConnected(p.getTelegramChatId() != null)
                .build();
    }

    @Override
    public NotificationSettingsResponse updateNotificationSettings(UUID practitionerId, NotificationSettingsRequest request) {
        Practitioner p = findPractitioner(practitionerId);
        if (request.getSessionRemindersEnabled() != null) p.setSessionRemindersEnabled(request.getSessionRemindersEnabled());
        if (request.getInactiveClientReminderDays() != null) p.setInactiveClientReminderDays(request.getInactiveClientReminderDays());
        practitionerRepository.save(p);
        return getNotificationSettings(practitionerId);
    }

    private Practitioner findPractitioner(UUID id) {
        return practitionerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Практик не найден"));
    }

    private ProfileSettingsResponse toProfileResponse(Practitioner p) {
        return ProfileSettingsResponse.builder()
                .id(p.getId())
                .email(p.getEmail())
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .phone(p.getPhone())
                .bio(p.getBio())
                .timezone(p.getTimezone())
                .defaultSessionDurationMin(p.getDefaultSessionDurationMin())
                .telegramChatId(p.getTelegramChatId())
                .telegramConnected(p.getTelegramChatId() != null)
                .plan(p.getPlan() != null ? p.getPlan().name() : "FREE")
                .planExpiresAt(p.getPlanExpiresAt())
                .build();
    }

    private BookingSettingsResponse toBookingResponse(BookingSettings s) {
        List<BookingServiceItemDto> services = Collections.emptyList();
        if (s.getServices() != null && !s.getServices().isBlank()) {
            try {
                services = MAPPER.readValue(s.getServices(), new TypeReference<>() {});
            } catch (Exception ignored) {}
        }
        String bookingUrl = s.getSlug() != null ? "https://regreso.app/book/" + s.getSlug() : null;
        return BookingSettingsResponse.builder()
                .isEnabled(Boolean.TRUE.equals(s.getIsEnabled()))
                .slug(s.getSlug())
                .bookingUrl(bookingUrl)
                .defaultDurationMin(s.getDefaultDurationMin())
                .bufferMin(s.getBufferMin())
                .advanceDays(s.getAdvanceDays())
                .requireIntakeForm(Boolean.TRUE.equals(s.getRequireIntakeForm()))
                .services(services)
                .welcomeMessage(s.getWelcomeMessage())
                .build();
    }
}
