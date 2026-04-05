package com.epam.rd.autocode.spring.project.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientDTO {

    @NotBlank(message = "Email є обов'язковим")
    @Email(message = "Некоректний формат Email")
    private String email;

    @NotBlank(message = "Пароль є обов'язковим")
    @Size(min = 6, message = "Пароль повинен містити не менше 6 символів")
    private String password;

    @NotBlank(message = "Ім'я не може бути порожнім")
    private String name;

    @NotNull(message = "Баланс є обов'язковим")
    @PositiveOrZero(message = "Баланс не може бути від'ємним")
    private BigDecimal balance;
}