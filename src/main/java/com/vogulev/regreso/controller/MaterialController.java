package com.vogulev.regreso.controller;

import com.vogulev.regreso.dto.request.MaterialRequest;
import com.vogulev.regreso.dto.response.MaterialListItemResponse;
import com.vogulev.regreso.dto.response.MaterialResponse;
import com.vogulev.regreso.security.PractitionerDetails;
import com.vogulev.regreso.service.MaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @GetMapping
    public ResponseEntity<List<MaterialListItemResponse>> getMaterials(
            @RequestParam(defaultValue = "false") boolean includeArchived,
            @AuthenticationPrincipal PractitionerDetails user) {
        return ResponseEntity.ok(materialService.getMaterials(user.getId(), includeArchived));
    }

    @PostMapping
    public ResponseEntity<MaterialResponse> createMaterial(
            @Valid @RequestBody MaterialRequest request,
            @AuthenticationPrincipal PractitionerDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(materialService.createMaterial(request, user.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaterialResponse> updateMaterial(
            @PathVariable UUID id,
            @Valid @RequestBody MaterialRequest request,
            @AuthenticationPrincipal PractitionerDetails user) {
        return ResponseEntity.ok(materialService.updateMaterial(id, request, user.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archiveMaterial(
            @PathVariable UUID id,
            @AuthenticationPrincipal PractitionerDetails user) {
        materialService.archiveMaterial(id, user.getId());
        return ResponseEntity.ok().build();
    }
}
