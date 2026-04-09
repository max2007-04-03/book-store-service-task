package com.epam.rd.autocode.spring.project.dto;

import com.epam.rd.autocode.spring.project.validation.UniqueEmail;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientDTO {



    @NotBlank(message = "Пароль є обов'язковим")
    @Size(min = 6, message = "Пароль повинен містити не менше 6 символів")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{6,}$",
            message = "Пароль має містити хоча б одну велику літеру, одну малу та цифру")
    private String password;

    @NotBlank(message = "Ім'я не може бути порожнім")
    private String name;


    @PositiveOrZero(message = "Баланс не може бути від'ємним")
    private BigDecimal balance = BigDecimal.ZERO;

    @NotBlank(message = "Email не може бути пустим")
    @Email(message = "Некоректний формат email")
    @UniqueEmail
    private String email;

    private String phone;

    private LocalDate birthDate;
}