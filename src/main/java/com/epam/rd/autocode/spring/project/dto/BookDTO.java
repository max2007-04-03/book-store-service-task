package com.epam.rd.autocode.spring.project.dto;

import com.epam.rd.autocode.spring.project.model.enums.AgeGroup;
import com.epam.rd.autocode.spring.project.model.enums.Language;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookDTO {

    @NotBlank(message = "Назва книги не може бути порожньою")
    private String name;

    private String genre;

    // Змінено тип зі String на оригінальний Enum
    private AgeGroup ageGroup;

    @NotNull(message = "Ціна є обов'язковою")
    @Positive(message = "Ціна має бути більшою за нуль")
    private BigDecimal price;

    @PastOrPresent(message = "Дата публікації не може бути в майбутньому")
    private LocalDate publicationDate;

    @NotBlank(message = "Автор не може бути порожнім")
    private String author;

    @Positive(message = "Кількість сторінок має бути більшою за нуль")
    private Integer pages;

    private String characteristics;

    private String description;

    private Language language;
}