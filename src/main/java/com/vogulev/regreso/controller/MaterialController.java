package com.vogulev.regreso.controller;

import com.vogulev.regreso.dto.request.MaterialRequest;
import com.vogulev.regreso.dto.response.MaterialListItemResponse;
import com.vogulev.regreso.dto.response.MaterialResponse;
import com.vogulev.regreso.security.PractitionerDetails;
import com.vogulev.regreso.service.MaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MaterialResponse> createMaterialJson(
            @Valid @RequestBody MaterialRequest request,
            @AuthenticationPrincipal PractitionerDetails user) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(materialService.createMaterial(request, null, user.getId()));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MaterialResponse> createMaterialMultipart(
            @Valid @ModelAttribute MaterialRequest request,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal PractitionerDetails user) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(materialService.createMaterial(request, file, user.getId()));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MaterialResponse> updateMaterialJson(
            @PathVariable UUID id,
            @Valid @RequestBody MaterialRequest request,
            @AuthenticationPrincipal PractitionerDetails user) throws IOException {
        return ResponseEntity.ok(materialService.updateMaterial(id, request, null, user.getId()));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MaterialResponse> updateMaterialMultipart(
            @PathVariable UUID id,
            @Valid @ModelAttribute MaterialRequest request,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal PractitionerDetails user) throws IOException {
        return ResponseEntity.ok(materialService.updateMaterial(id, request, file, user.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archiveMaterial(
            @PathVariable UUID id,
            @AuthenticationPrincipal PractitionerDetails user) {
        materialService.archiveMaterial(id, user.getId());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/unarchive")
    public ResponseEntity<Void> unarchiveMaterial(
            @PathVariable UUID id,
            @AuthenticationPrincipal PractitionerDetails user) {
        materialService.unarchiveMaterial(id, user.getId());
        return ResponseEntity.ok().build();
    }
}
