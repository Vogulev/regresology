package com.vogulev.regreso.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Сервис для хранения и удаления загружаемых файлов (фото, сертификаты).
 * Реализация может быть локальной или облачной (S3/MinIO).
 */
public interface FileStorageService {

    /**
     * Сохраняет файл в указанную директорию и возвращает публичный URL для доступа к нему.
     *
     * @param file   загружаемый файл
     * @param folder имя поддиректории (например, "photos" или "certificates")
     * @return публичный URL сохранённого файла (например, /api/files/photos/uuid.jpg)
     * @throws IOException если не удалось сохранить файл
     */
    String store(MultipartFile file, String folder) throws IOException;

    /**
     * Удаляет файл по его публичному URL.
     * Если файл не найден — игнорирует ошибку.
     *
     * @param fileUrl публичный URL файла, возвращённый методом {@link #store}
     */
    void delete(String fileUrl);
}
