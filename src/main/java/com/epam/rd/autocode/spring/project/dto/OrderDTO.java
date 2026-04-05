package com.epam.rd.autocode.spring.project.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {

    @NotBlank(message = "Email клієнта є обов'язковим")
    private String clientEmail;

    // Email працівника може бути порожнім, поки замовлення не підтверджено
    private String employeeEmail;

    private LocalDateTime orderDate;

    @PositiveOrZero(message = "Вартість замовлення не може бути від'ємною")
    private BigDecimal price;

    @NotEmpty(message = "Замовлення повинно містити хоча б одну книгу")
    private List<BookItemDTO> bookItems;
}