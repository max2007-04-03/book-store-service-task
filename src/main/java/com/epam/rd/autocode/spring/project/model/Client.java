package com.epam.rd.autocode.spring.project.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "CLIENTS")
public class Client extends User {

    @Builder.Default
    @Column(name = "BALANCE")
    private BigDecimal balance = BigDecimal.ZERO;

    private String phone;

    private java.time.LocalDate birthDate;

    public Client(Long id, String name, String email, String password, BigDecimal balance) {
        super(id, name, email, password);
        this.balance = (balance == null) ? BigDecimal.ZERO : balance;
    }

}