package com.epam.rd.autocode.spring.project.validation;

import com.epam.rd.autocode.spring.project.repo.ClientRepository;
import com.epam.rd.autocode.spring.project.repo.EmployeeRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, String> {

    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.isEmpty()) {
            return true;
        }

        boolean clientExists = clientRepository.findByEmail(email).isPresent();
        boolean employeeExists = employeeRepository.findByEmail(email).isPresent();

        return !clientExists && !employeeExists;
    }
}