package com.epam.rd.autocode.spring.project.exception;

public class InvalidProfileDataException extends RuntimeException {
    public InvalidProfileDataException(String message) {
        super(message);
    }
}