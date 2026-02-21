package com.vogulev.regreso.controller;

import com.vogulev.regreso.dto.request.ArchiveRequest;
import com.vogulev.regreso.dto.request.ClientRequest;
import com.vogulev.regreso.dto.request.UpdateProgressRequest;
import com.vogulev.regreso.dto.response.ClientListItemResponse;
import com.vogulev.regreso.dto.response.ClientResponse;
import com.vogulev.regreso.dto.response.SessionListItemResponse;
import com.vogulev.regreso.security.PractitionerDetails;
import com.vogulev.regreso.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    public ResponseEntity<List<ClientListItemResponse>> getClients(
            @AuthenticationPrincipal PractitionerDetails user) {
        return ResponseEntity.ok(clientService.getClients(user.getId()));
    }

    @PostMapping
    public ResponseEntity<ClientResponse> createClient(
            @Valid @RequestBody ClientRequest request,
            @AuthenticationPrincipal PractitionerDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clientService.createClient(request, user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> getClient(
            @PathVariable UUID id,
            @AuthenticationPrincipal PractitionerDetails user) {
        return ResponseEntity.ok(clientService.getClient(id, user.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientResponse> updateClient(
            @PathVariable UUID id,
            @Valid @RequestBody ClientRequest request,
            @AuthenticationPrincipal PractitionerDetails user) {
        return ResponseEntity.ok(clientService.updateClient(id, request, user.getId()));
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<Void> archiveClient(
            @PathVariable UUID id,
            @RequestBody ArchiveRequest request,
            @AuthenticationPrincipal PractitionerDetails user) {
        clientService.archiveClient(id, request.isArchive(), user.getId());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/progress")
    public ResponseEntity<Void> updateProgress(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProgressRequest request,
            @AuthenticationPrincipal PractitionerDetails user) {
        clientService.updateProgress(id, request.getOverallProgress(), user.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/sessions")
    public ResponseEntity<List<SessionListItemResponse>> getClientSessions(
            @PathVariable UUID id,
            @AuthenticationPrincipal PractitionerDetails user) {
        return ResponseEntity.ok(clientService.getClientSessions(id, user.getId()));
    }
}
