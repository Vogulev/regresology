package com.vogulev.regreso.controller;

import com.vogulev.regreso.service.impl.LocalFileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Контроллер для отдачи загруженных файлов с локального диска.
 * Активен только при {@code storage.type=local}.
 * При использовании MinIO файлы отдаются напрямую из бакета — этот контроллер не нужен.
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class FileController {

    private final LocalFileStorageService fileStorageService;

    @GetMapping("/{folder}/{filename}")
    public ResponseEntity<Resource> serveFile(
            @PathVariable String folder,
            @PathVariable String filename) throws IOException {

        // Защита от path traversal
        if (filename.contains("..") || folder.contains("..")) {
            return ResponseEntity.badRequest().build();
        }

        Path filePath = fileStorageService.resolveFilePath(folder, filename);
        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource;
        try {
            resource = new UrlResource(filePath.toUri());
        } catch (MalformedURLException e) {
            return ResponseEntity.notFound().build();
        }

        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}
