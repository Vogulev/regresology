package com.vogulev.regreso.service.impl;

import com.vogulev.regreso.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Реализация {@link FileStorageService} с хранением на локальном диске.
 * Активна при {@code storage.type=local} (по умолчанию).
 * Файлы сохраняются в директорию {@code upload.dir} (~/regreso-uploads/).
 * Отдача файлов — через FileController по пути /api/files/{folder}/{filename}.
 * <p>
 * <b>Не подходит для production-деплоя</b>: файлы теряются при перезапуске контейнера.
 * Для production используйте {@code storage.type=minio}.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    @Value("${upload.dir:${user.home}/regreso-uploads}")
    private String uploadDir;

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(uploadDir));
        log.info("File storage initialized at: {}", uploadDir);
    }

    @Override
    public String store(MultipartFile file, String folder) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        String storedFilename = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);

        Path dir = Paths.get(uploadDir, folder);
        Files.createDirectories(dir);

        Path target = dir.resolve(storedFilename);
        file.transferTo(target.toFile());

        log.debug("Stored file: {}/{}", folder, storedFilename);
        return "/api/files/" + folder + "/" + storedFilename;
    }

    @Override
    public void delete(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/api/files/")) {
            return;
        }
        String relativePath = fileUrl.substring("/api/files/".length());
        Path target = Paths.get(uploadDir).resolve(relativePath);
        try {
            Files.deleteIfExists(target);
            log.debug("Deleted file: {}", relativePath);
        } catch (IOException e) {
            log.warn("Failed to delete file {}: {}", relativePath, e.getMessage());
        }
    }

    public Path resolveFilePath(String folder, String filename) {
        return Paths.get(uploadDir, folder, filename);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
