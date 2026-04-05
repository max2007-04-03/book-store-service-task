package com.epam.rd.autocode.spring.project.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "CLIENTS")
public class Client extends User {

    @Column(name = "BALANCE")
    private BigDecimal balance;

    public Client(Long id, String name, String email, String password, BigDecimal balance) {
        super(id, name, email, password);
        this.balance = balance;
    }
}