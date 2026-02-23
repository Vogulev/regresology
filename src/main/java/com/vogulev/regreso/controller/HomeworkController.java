package com.vogulev.regreso.controller;

import com.vogulev.regreso.dto.request.CreateHomeworkRequest;
import com.vogulev.regreso.dto.request.UpdateHomeworkStatusRequest;
import com.vogulev.regreso.dto.response.HomeworkResponse;
import com.vogulev.regreso.security.PractitionerDetails;
import com.vogulev.regreso.service.HomeworkService;
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
public class HomeworkController {

    private final HomeworkService homeworkService;

    @GetMapping("/api/clients/{clientId}/homework")
    public ResponseEntity<List<HomeworkResponse>> getClientHomework(
            @PathVariable UUID clientId,
            @AuthenticationPrincipal PractitionerDetails user) {
        return ResponseEntity.ok(homeworkService.getClientHomework(clientId, user.getId()));
    }

    @PostMapping("/api/clients/{clientId}/homework")
    public ResponseEntity<HomeworkResponse> createForClient(
            @PathVariable UUID clientId,
            @RequestBody CreateHomeworkRequest request,
            @AuthenticationPrincipal PractitionerDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(homeworkService.createForClient(clientId, request, user.getId()));
    }

    @PostMapping("/api/sessions/{sessionId}/homework")
    public ResponseEntity<HomeworkResponse> createForSession(
            @PathVariable UUID sessionId,
            @RequestBody CreateHomeworkRequest request,
            @AuthenticationPrincipal PractitionerDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(homeworkService.createForSession(sessionId, request, user.getId()));
    }

    @PatchMapping("/api/homework/{id}/status")
    public ResponseEntity<HomeworkResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateHomeworkStatusRequest request,
            @AuthenticationPrincipal PractitionerDetails user) {
        return ResponseEntity.ok(homeworkService.updateStatus(id, request.getStatus(), user.getId()));
    }
}
