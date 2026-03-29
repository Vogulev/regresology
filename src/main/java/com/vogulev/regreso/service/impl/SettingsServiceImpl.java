package com.vogulev.regreso.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vogulev.regreso.dto.request.BookingSettingsRequest;
import com.vogulev.regreso.dto.request.NotificationSettingsRequest;
import com.vogulev.regreso.dto.request.ProfileSettingsRequest;
import com.vogulev.regreso.dto.response.BookingAvailabilityMode;
import com.vogulev.regreso.dto.response.BookingDayAvailabilityDto;
import com.vogulev.regreso.dto.response.BookingServiceItemDto;
import com.vogulev.regreso.dto.response.BookingSettingsResponse;
import com.vogulev.regreso.dto.response.CertificateResponse;
import com.vogulev.regreso.dto.response.NotificationSettingsResponse;
import com.vogulev.regreso.dto.response.ProfileSettingsResponse;
import com.vogulev.regreso.entity.BookingSettings;
import com.vogulev.regreso.entity.Certificate;
import com.vogulev.regreso.entity.Practitioner;
import com.vogulev.regreso.exception.ResourceNotFoundException;
import com.vogulev.regreso.repository.BookingSettingsRepository;
import com.vogulev.regreso.repository.CertificateRepository;
import com.vogulev.regreso.repository.PractitionerRepository;
import com.vogulev.regreso.service.FileStorageService;
import com.vogulev.regreso.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private final CertificateRepository certificateRepository;
    private final FileStorageService fileStorageService;

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
                        .availabilityMode(BookingAvailabilityMode.DEFAULT)
                        .weeklyAvailability(Collections.emptyList())
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
        if (request.getAvailabilityMode() != null) {
            settings.setAvailabilityMode(request.getAvailabilityMode().name());
        }
        if (request.getWeeklyAvailability() != null) {
            try {
                settings.setWeeklyAvailability(MAPPER.writeValueAsString(request.getWeeklyAvailability()));
            } catch (Exception e) {
                settings.setWeeklyAvailability(null);
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
                .practitionerSessionRemindersEnabled(Boolean.TRUE.equals(p.getPractitionerSessionRemindersEnabled()))
                .inactiveClientReminderDays(p.getInactiveClientReminderDays())
                .telegramConnected(p.getTelegramChatId() != null)
                .build();
    }

    @Override
    public NotificationSettingsResponse updateNotificationSettings(UUID practitionerId, NotificationSettingsRequest request) {
        Practitioner p = findPractitioner(practitionerId);
        if (request.getSessionRemindersEnabled() != null) p.setSessionRemindersEnabled(request.getSessionRemindersEnabled());
        if (request.getPractitionerSessionRemindersEnabled() != null) {
            p.setPractitionerSessionRemindersEnabled(request.getPractitionerSessionRemindersEnabled());
        }
        if (request.getInactiveClientReminderDays() != null) p.setInactiveClientReminderDays(request.getInactiveClientReminderDays());
        practitionerRepository.save(p);
        return getNotificationSettings(practitionerId);
    }

    private Practitioner findPractitioner(UUID id) {
        return practitionerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Практик не найден"));
    }

    @Override
    public ProfileSettingsResponse uploadPhoto(UUID practitionerId, MultipartFile file) throws IOException {
        Practitioner p = findPractitioner(practitionerId);
        String oldPhotoUrl = p.getPhotoUrl();
        String newPhotoUrl = fileStorageService.store(file, "photos");
        p.setPhotoUrl(newPhotoUrl);
        practitionerRepository.save(p);
        if (oldPhotoUrl != null) {
            fileStorageService.delete(oldPhotoUrl);
        }
        return toProfileResponse(p);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CertificateResponse> getCertificates(UUID practitionerId) {
        return certificateRepository.findByPractitionerIdOrderByCreatedAtDesc(practitionerId)
                .stream()
                .map(this::toCertificateResponse)
                .toList();
    }

    @Override
    public CertificateResponse uploadCertificate(UUID practitionerId, String name, MultipartFile file) throws IOException {
        Practitioner practitioner = findPractitioner(practitionerId);
        String fileUrl = fileStorageService.store(file, "certificates");
        Certificate cert = Certificate.builder()
                .practitioner(practitioner)
                .name(name)
                .fileUrl(fileUrl)
                .originalFilename(file.getOriginalFilename())
                .build();
        return toCertificateResponse(certificateRepository.save(cert));
    }

    @Override
    public void deleteCertificate(UUID practitionerId, UUID certificateId) {
        Certificate cert = certificateRepository.findByIdAndPractitionerId(certificateId, practitionerId)
                .orElseThrow(() -> new ResourceNotFoundException("Сертификат не найден"));
        certificateRepository.delete(cert);
        fileStorageService.delete(cert.getFileUrl());
    }

    private ProfileSettingsResponse toProfileResponse(Practitioner p) {
        return ProfileSettingsResponse.builder()
                .id(p.getId())
                .email(p.getEmail())
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .phone(p.getPhone())
                .bio(p.getBio())
                .photoUrl(p.getPhotoUrl())
                .timezone(p.getTimezone())
                .defaultSessionDurationMin(p.getDefaultSessionDurationMin())
                .telegramChatId(p.getTelegramChatId())
                .telegramConnected(p.getTelegramChatId() != null)
                .plan(p.getPlan() != null ? p.getPlan().name() : "FREE")
                .planExpiresAt(p.getPlanExpiresAt())
                .build();
    }

    private CertificateResponse toCertificateResponse(Certificate c) {
        return CertificateResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .fileUrl(c.getFileUrl())
                .originalFilename(c.getOriginalFilename())
                .createdAt(c.getCreatedAt())
                .build();
    }

    private BookingSettingsResponse toBookingResponse(BookingSettings s) {
        List<BookingServiceItemDto> services = readJsonList(s.getServices(), new TypeReference<>() {});
        List<BookingDayAvailabilityDto> weeklyAvailability = readJsonList(s.getWeeklyAvailability(), new TypeReference<>() {});
        String bookingUrl = s.getSlug() != null ? "https://regreso.app/book/" + s.getSlug() : null;
        BookingAvailabilityMode availabilityMode = BookingAvailabilityMode.DEFAULT;
        if (s.getAvailabilityMode() != null && !s.getAvailabilityMode().isBlank()) {
            try {
                availabilityMode = BookingAvailabilityMode.valueOf(s.getAvailabilityMode());
            } catch (IllegalArgumentException ignored) {
                availabilityMode = BookingAvailabilityMode.DEFAULT;
            }
        }
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
                .availabilityMode(availabilityMode)
                .weeklyAvailability(weeklyAvailability)
                .build();
    }

    private <T> List<T> readJsonList(String json, TypeReference<List<T>> typeReference) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(json, typeReference);
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }
}
