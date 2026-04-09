package com.epam.rd.autocode.spring.project.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniqueEmailValidator.class)
@Documented
public @interface UniqueEmail {
    String message() default "Цей email вже зареєстрований у системі";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}