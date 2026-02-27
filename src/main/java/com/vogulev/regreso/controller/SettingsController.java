package com.vogulev.regreso.controller;

import com.vogulev.regreso.dto.request.BookingSettingsRequest;
import com.vogulev.regreso.dto.request.NotificationSettingsRequest;
import com.vogulev.regreso.dto.request.ProfileSettingsRequest;
import com.vogulev.regreso.dto.response.BookingSettingsResponse;
import com.vogulev.regreso.dto.response.NotificationSettingsResponse;
import com.vogulev.regreso.dto.response.ProfileSettingsResponse;
import com.vogulev.regreso.dto.response.TelegramLinkResponse;
import com.vogulev.regreso.security.PractitionerDetails;
import com.vogulev.regreso.service.SettingsService;
import com.vogulev.regreso.service.TelegramLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final TelegramLinkService telegramLinkService;
    private final SettingsService settingsService;

    @GetMapping("/telegram/link")
    public ResponseEntity<TelegramLinkResponse> getTelegramLink(
            @AuthenticationPrincipal PractitionerDetails user) {

        var practitioner = user.getPractitioner();
        String link = telegramLinkService.generatePractitionerLink(practitioner.getId());

        return ResponseEntity.ok(TelegramLinkResponse.builder()
                .botUrl(link)
                .connected(practitioner.getTelegramChatId() != null)
                .telegramChatId(practitioner.getTelegramChatId())
                .build());
    }

    @GetMapping("/profile")
    public ResponseEntity<ProfileSettingsResponse> getProfile(
            @AuthenticationPrincipal PractitionerDetails user) {
        return ResponseEntity.ok(settingsService.getProfile(user.getId()));
    }

    @PutMapping("/profile")
    public ResponseEntity<ProfileSettingsResponse> updateProfile(
            @AuthenticationPrincipal PractitionerDetails user,
            @RequestBody ProfileSettingsRequest request) {
        return ResponseEntity.ok(settingsService.updateProfile(user.getId(), request));
    }

    @GetMapping("/booking")
    public ResponseEntity<BookingSettingsResponse> getBookingSettings(
            @AuthenticationPrincipal PractitionerDetails user) {
        return ResponseEntity.ok(settingsService.getBookingSettings(user.getId()));
    }

    @PutMapping("/booking")
    public ResponseEntity<BookingSettingsResponse> updateBookingSettings(
            @AuthenticationPrincipal PractitionerDetails user,
            @RequestBody BookingSettingsRequest request) {
        return ResponseEntity.ok(settingsService.updateBookingSettings(user.getId(), request));
    }

    @GetMapping("/notifications")
    public ResponseEntity<NotificationSettingsResponse> getNotificationSettings(
            @AuthenticationPrincipal PractitionerDetails user) {
        return ResponseEntity.ok(settingsService.getNotificationSettings(user.getId()));
    }

    @PutMapping("/notifications")
    public ResponseEntity<NotificationSettingsResponse> updateNotificationSettings(
            @AuthenticationPrincipal PractitionerDetails user,
            @RequestBody NotificationSettingsRequest request) {
        return ResponseEntity.ok(settingsService.updateNotificationSettings(user.getId(), request));
    }
}
