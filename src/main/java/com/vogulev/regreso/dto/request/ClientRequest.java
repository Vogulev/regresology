package com.vogulev.regreso.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ClientRequest {

    @NotBlank(message = "Имя обязательно")
    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Некорректный номер телефона")
    private String phone;

    @Email(message = "Некорректный email")
    private String email;

    private LocalDate birthDate;

    @Size(max = 100)
    private String telegramUsername;

    private String initialRequest;

    private List<String> presentingIssues;

    private Boolean hasContraindications;

    private String contraindicationsNotes;

    private Boolean intakeFormCompleted;

    private String generalNotes;
}
