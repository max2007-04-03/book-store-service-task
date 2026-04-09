package com.epam.rd.autocode.spring.project.dto;

import com.epam.rd.autocode.spring.project.validation.UniqueEmail;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDTO {

    @NotBlank(message = "Пароль є обов'язковим")
    @Size(min = 6, message = "Пароль повинен містити не менше 6 символів")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{6,}$",
            message = "Пароль має містити хоча б одну велику літеру, одну малу та цифру")
    private String password;

    @NotBlank(message = "Ім'я не може бути порожнім")
    private String name;

    @Past(message = "Дата народження має бути в минулому")
    private LocalDate birthDate;

    @Pattern(regexp = "^\\+?[0-9\\-\\s]+$", message = "Некоректний формат телефону")
    private String phone;

    @NotBlank(message = "Email не може бути пустим")
    @Email(message = "Некоректний формат email")
    @UniqueEmail
    private String email;
}
