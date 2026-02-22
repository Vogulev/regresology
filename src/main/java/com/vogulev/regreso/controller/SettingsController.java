package com.vogulev.regreso.controller;

import com.vogulev.regreso.dto.response.TelegramLinkResponse;
import com.vogulev.regreso.security.PractitionerDetails;
import com.vogulev.regreso.service.TelegramLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final TelegramLinkService telegramLinkService;

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
}
