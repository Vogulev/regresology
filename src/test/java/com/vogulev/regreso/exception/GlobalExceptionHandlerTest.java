package com.vogulev.regreso.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("превышение размера файла возвращает 413 и понятное сообщение")
    void maxUploadSizeExceeded_returnsPayloadTooLarge() {
        ResponseEntity<ErrorResponse> response = handler.handleMaxUploadSizeExceeded(
                new MaxUploadSizeExceededException(10 * 1024 * 1024)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Файл слишком большой. Максимальный размер файла — 10 МБ");
    }

    @Test
    @DisplayName("ошибка файлового хранилища возвращает 503 и понятное сообщение")
    void ioException_returnsServiceUnavailable() {
        ResponseEntity<ErrorResponse> response = handler.handleIOException(
                new IOException("MinIO storage is unavailable")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Не удалось сохранить файл. Попробуйте позже");
    }
}
