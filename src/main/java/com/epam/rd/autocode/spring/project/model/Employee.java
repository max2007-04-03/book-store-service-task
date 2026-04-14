package com.epam.rd.autocode.spring.project.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "EMPLOYEES")
public class Employee extends User {

    @Column(name = "BIRTH_DATE")
    private LocalDate birthDate;

    @Column(name = "PHONE")
    private String phone;

    public Employee(Long id, String name, String email, String password, LocalDate birthDate, String phone) {
        super(id, name, email, password);
        this.birthDate = birthDate;
        this.phone = phone;
    }

}