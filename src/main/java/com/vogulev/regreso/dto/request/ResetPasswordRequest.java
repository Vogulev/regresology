package com.vogulev.regreso.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный email")
    private String email;

    @NotBlank(message = "Код обязателен")
    @Pattern(regexp = "^\\d{6}$", message = "Код должен содержать 6 цифр")
    @Size(min = 6, max = 6, message = "Код должен содержать 6 цифр")
    private String code;

    @NotBlank(message = "Новый пароль обязателен")
    @Size(min = 8, message = "Пароль должен содержать минимум 8 символов")
    private String newPassword;
}
