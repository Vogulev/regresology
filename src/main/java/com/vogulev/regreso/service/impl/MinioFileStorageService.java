package com.vogulev.regreso.service.impl;

import com.vogulev.regreso.service.FileStorageService;
import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Реализация {@link FileStorageService} с хранением в MinIO (S3-совместимое хранилище).
 * Активна при {@code storage.type=minio}.
 * <p>
 * Файлы сохраняются в бакет {@code storage.minio.bucket}.
 * Возвращаемый URL — прямой публичный URL бакета MinIO, без проксирования через Spring.
 * При деплое убедитесь что бакет настроен как публичный (или используйте presigned URL).
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "minio")
public class MinioFileStorageService implements FileStorageService {

    /** Внутренний адрес MinIO для подключения бэкенда (например, http://minio:9000 в Docker). */
    @Value("${storage.minio.endpoint}")
    private String endpoint;

    /**
     * Публичный адрес MinIO для формирования URL, которые открывает браузер.
     * В Docker отличается от endpoint: браузер не знает про hostname "minio".
     * По умолчанию совпадает с endpoint (удобно для локальной разработки без Docker).
     */
    @Value("${storage.minio.public-url:${storage.minio.endpoint}}")
    private String publicUrl;

    @Value("${storage.minio.access-key}")
    private String accessKey;

    @Value("${storage.minio.secret-key}")
    private String secretKey;

    @Value("${storage.minio.bucket}")
    private String bucket;

    private MinioClient minioClient;
    private boolean available;

    @PostConstruct
    public void init() {
        minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        available = ensureBucketExists();
        if (available) {
            log.info("MinIO storage initialized. Endpoint: {}, PublicUrl: {}, Bucket: {}", endpoint, publicUrl, bucket);
        } else {
            log.error("MinIO storage is unavailable. Endpoint: {}, PublicUrl: {}, Bucket: {}", endpoint, publicUrl, bucket);
        }
    }

    @Override
    public String store(MultipartFile file, String folder) throws IOException {
        if (!available) {
            throw new IOException("MinIO storage is unavailable");
        }

        String extension = getExtension(file.getOriginalFilename());
        String objectName = folder + "/" + UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);

        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                    .build());

            log.debug("Stored to MinIO: {}/{}", bucket, objectName);
            return buildPublicUrl(objectName);
        } catch (MinioException e) {
            log.error("MinIO store failed for object {}: {}", objectName, e.getMessage());
            throw new IOException("Не удалось сохранить файл в хранилище: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IOException("Ошибка хранилища: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;
        String objectName = extractObjectName(fileUrl);
        if (objectName == null) {
            log.warn("Cannot extract object name from URL: {}", fileUrl);
            return;
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
            log.debug("Deleted from MinIO: {}/{}", bucket, objectName);
        } catch (Exception e) {
            log.warn("Failed to delete MinIO object {}: {}", objectName, e.getMessage());
        }
    }

    /**
     * Формирует публичный URL по адресу {@code publicUrl} — тот, что открывает браузер.
     * Формат: {publicUrl}/{bucket}/{objectName}
     */
    private String buildPublicUrl(String objectName) {
        String base = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
        return base + "/" + bucket + "/" + objectName;
    }

    /**
     * Извлекает имя объекта из URL.
     * Поддерживает оба формата: через publicUrl и через endpoint.
     */
    private String extractObjectName(String fileUrl) {
        for (String base : new String[]{publicUrl, endpoint}) {
            String normalised = base.endsWith("/") ? base : base + "/";
            String bucketPrefix = normalised + bucket + "/";
            if (fileUrl.startsWith(bucketPrefix)) {
                return fileUrl.substring(bucketPrefix.length());
            }
        }
        return null;
    }

    private boolean ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                // Установить публичную политику чтения
                minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                        .bucket(bucket)
                        .config(buildPublicReadPolicy(bucket))
                        .build());
                log.info("Created MinIO bucket: {}", bucket);
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to initialize MinIO bucket '{}': {}", bucket, e.getMessage(), e);
            return false;
        }
    }

    /**
     * JSON-политика публичного чтения всех объектов бакета (GET без авторизации).
     */
    private String buildPublicReadPolicy(String bucketName) {
        return """
                {
                  "Version": "2012-10-17",
                  "Statement": [{
                    "Effect": "Allow",
                    "Principal": {"AWS": ["*"]},
                    "Action": ["s3:GetObject"],
                    "Resource": ["arn:aws:s3:::%s/*"]
                  }]
                }
                """.formatted(bucketName);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
