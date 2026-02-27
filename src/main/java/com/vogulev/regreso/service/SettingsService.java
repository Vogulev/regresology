package com.vogulev.regreso.service;

import com.vogulev.regreso.dto.request.BookingSettingsRequest;
import com.vogulev.regreso.dto.request.NotificationSettingsRequest;
import com.vogulev.regreso.dto.request.ProfileSettingsRequest;
import com.vogulev.regreso.dto.response.BookingSettingsResponse;
import com.vogulev.regreso.dto.response.NotificationSettingsResponse;
import com.vogulev.regreso.dto.response.ProfileSettingsResponse;

import java.util.UUID;

public interface SettingsService {
    ProfileSettingsResponse getProfile(UUID practitionerId);
    ProfileSettingsResponse updateProfile(UUID practitionerId, ProfileSettingsRequest request);
    BookingSettingsResponse getBookingSettings(UUID practitionerId);
    BookingSettingsResponse updateBookingSettings(UUID practitionerId, BookingSettingsRequest request);
    NotificationSettingsResponse getNotificationSettings(UUID practitionerId);
    NotificationSettingsResponse updateNotificationSettings(UUID practitionerId, NotificationSettingsRequest request);
}
