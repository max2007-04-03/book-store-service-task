package com.epam.rd.autocode.spring.project.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookItemDTO {

    @NotBlank(message = "Назва книги є обов'язковою")
    private String bookName;

    @NotNull(message = "Кількість є обов'язковою")
    @Min(value = 1, message = "Кількість має бути не менше 1")
    private Integer quantity;
}