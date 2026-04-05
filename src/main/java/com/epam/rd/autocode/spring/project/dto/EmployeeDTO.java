package com.epam.rd.autocode.spring.project.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDTO {

    @NotBlank(message = "Email є обов'язковим")
    @Email(message = "Некоректний формат Email")
    private String email;

    @NotBlank(message = "Пароль є обов'язковим")
    private String password;

    @NotBlank(message = "Ім'я не може бути порожнім")
    private String name;

    @Past(message = "Дата народження має бути в минулому")
    private LocalDate birthDate;

    @Pattern(regexp = "^\\+?[0-9\\-\\s]+$", message = "Некоректний формат телефону")
    private String phone;
}