package com.epam.rd.autocode.spring.project.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    public static class InsufficientBalanceException extends RuntimeException {
        public InsufficientBalanceException(String message) {
            super(message);
        }
    }

    @ExceptionHandler(NotFoundException.class)
    public String handleNotFound(NotFoundException ex, Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException ex, Model model) {
        model.addAttribute("errorMessage", "У вас немає прав для перегляду цієї сторінки.");
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneralError(Exception ex, Model model) {
        model.addAttribute("errorMessage", "Сталася внутрішня помилка сервера. Спробуйте пізніше.");
        return "error";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDatabaseConflict(DataIntegrityViolationException ex, RedirectAttributes ra) {
        ra.addFlashAttribute("error", "Дія неможлива: ці дані пов'язані з іншими записами (наприклад, активними замовленнями).");
        return "redirect:/home";
    }

    @ExceptionHandler({IllegalArgumentException.class, AlreadyExistException.class})
    public String handleValidation(RuntimeException ex, RedirectAttributes ra, HttpServletRequest request) {
        ra.addFlashAttribute("error", ex.getMessage());
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/register");
    }

    @ExceptionHandler({InsufficientBalanceException.class, OutOfStockException.class})
    public String handleLogicErrors(RuntimeException ex, RedirectAttributes ra, HttpServletRequest request) {
        ra.addFlashAttribute("error", ex.getMessage());
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/home");
    }
}