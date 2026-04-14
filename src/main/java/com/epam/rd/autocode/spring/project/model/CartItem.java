package com.epam.rd.autocode.spring.project.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String clientEmail;
    private String bookName;
    private BigDecimal price;
    private int quantity;

    public BigDecimal getTotalPrice() {
        return price != null ? price.multiply(new BigDecimal(quantity)) : BigDecimal.ZERO;
    }

}