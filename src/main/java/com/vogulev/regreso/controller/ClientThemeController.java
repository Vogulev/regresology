package com.vogulev.regreso.controller;

import com.vogulev.regreso.dto.request.CreateThemeRequest;
import com.vogulev.regreso.dto.request.LinkSessionToThemeRequest;
import com.vogulev.regreso.dto.response.ClientThemeResponse;
import com.vogulev.regreso.security.PractitionerDetails;
import com.vogulev.regreso.service.ClientThemeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ClientThemeController {

    private final ClientThemeService clientThemeService;

    @GetMapping("/api/clients/{clientId}/themes")
    public ResponseEntity<List<ClientThemeResponse>> getClientThemes(
            @PathVariable UUID clientId,
            @AuthenticationPrincipal PractitionerDetails user) {
        return ResponseEntity.ok(clientThemeService.getClientThemes(clientId, user.getId()));
    }

    @PostMapping("/api/clients/{clientId}/themes")
    public ResponseEntity<ClientThemeResponse> createTheme(
            @PathVariable UUID clientId,
            @Valid @RequestBody CreateThemeRequest request,
            @AuthenticationPrincipal PractitionerDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clientThemeService.createTheme(clientId, request, user.getId()));
    }

    @PostMapping("/api/themes/{themeId}/sessions")
    public ResponseEntity<ClientThemeResponse> linkSession(
            @PathVariable UUID themeId,
            @Valid @RequestBody LinkSessionToThemeRequest request,
            @AuthenticationPrincipal PractitionerDetails user) {
        return ResponseEntity.ok(clientThemeService.linkSession(themeId, request, user.getId()));
    }

    @PatchMapping("/api/themes/{themeId}/resolve")
    public ResponseEntity<ClientThemeResponse> resolveTheme(
            @PathVariable UUID themeId,
            @AuthenticationPrincipal PractitionerDetails user) {
        return ResponseEntity.ok(clientThemeService.resolveTheme(themeId, user.getId()));
    }
}
