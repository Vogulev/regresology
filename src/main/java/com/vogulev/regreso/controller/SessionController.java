package com.vogulev.regreso.controller;

import com.vogulev.regreso.dto.request.CancelSessionRequest;
import com.vogulev.regreso.dto.request.CreateSessionRequest;
import com.vogulev.regreso.dto.request.UpdateSessionRequest;
import com.vogulev.regreso.dto.response.SessionPrepResponse;
import com.vogulev.regreso.dto.response.SessionResponse;
import com.vogulev.regreso.dto.response.SessionSummaryStatusResponse;
import com.vogulev.regreso.security.PractitionerDetails;
import com.vogulev.regreso.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    public ResponseEntity<SessionResponse> createSession(
            @Valid @RequestBody CreateSessionRequest request,
            @AuthenticationPrincipal PractitionerDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sessionService.createSession(request, user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> getSession(
            @PathVariable UUID id,
            @AuthenticationPrincipal PractitionerDetails user) {
        return ResponseEntity.ok(sessionService.getSession(id, user.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SessionResponse> updateSession(
            @PathVariable UUID id,
            @RequestBody UpdateSessionRequest request,
            @AuthenticationPrincipal PractitionerDetails user) {
        return ResponseEntity.ok(sessionService.updateSession(id, request, user.getId()));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<SessionResponse> completeSession(
            @PathVariable UUID id,
            @RequestBody(required = false) UpdateSessionRequest request,
            @AuthenticationPrincipal PractitionerDetails user) {
        return ResponseEntity.ok(sessionService.completeSession(id, request, user.getId()));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<SessionResponse> cancelSession(
            @PathVariable UUID id,
            @RequestBody(required = false) CancelSessionRequest request,
            @AuthenticationPrincipal PractitionerDetails user) {
        return ResponseEntity.ok(sessionService.cancelSession(id, request, user.getId()));
    }

    @GetMapping("/{id}/prepare")
    public ResponseEntity<SessionPrepResponse> getSessionPrep(
            @PathVariable UUID id,
            @AuthenticationPrincipal PractitionerDetails user) {
        return ResponseEntity.ok(sessionService.getSessionPrep(id, user.getId()));
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<SessionSummaryStatusResponse> getSessionSummary(
            @PathVariable UUID id,
            @AuthenticationPrincipal PractitionerDetails user) {
        return ResponseEntity.ok(sessionService.getSessionSummary(id, user.getId()));
    }
}
