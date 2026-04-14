package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.EmployeeDTO;
import com.epam.rd.autocode.spring.project.exception.InvalidProfileDataException;
import com.epam.rd.autocode.spring.project.service.EmployeeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;

import java.security.Principal;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/employees")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EMPLOYEE')")
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public String getAllEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "email") String sortBy,
            Model model) {

        log.debug(" Запит списку працівників: сторінка={}, розмір={}, сортування={}", page, size, sortBy);

        PageRequest pageable = PageRequest.of(page, size, Sort.by(sortBy));
        Page<EmployeeDTO> employeePage = employeeService.getAllEmployees(pageable);

        model.addAttribute("employeePage", employeePage);
        model.addAttribute("employees", employeePage.getContent());

        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", employeePage.getTotalPages());

        return "employees/list";
    }

    @GetMapping("/{email}")
    public String getEmployeeDetails(@PathVariable String email, Model model) {
        model.addAttribute("employee", employeeService.getEmployeeByEmail(email));
        return "employees/detail";
    }

    @GetMapping("/{email}/edit")
    public String editEmployeeForm(@PathVariable String email, Model model, Principal principal) {
        if (principal == null || !principal.getName().equals(email)) {
            log.warn(" Безпека: {} намагається відкрити форму редагування чужого профілю ({})",
                    principal != null ? principal.getName() : "Гість", email);
            return "redirect:/employees";
        }

        model.addAttribute("employee", employeeService.getEmployeeByEmail(email));
        return "employees/employee-edit";
    }

    @PostMapping("/{email}/update")
    public String updateEmployee(@PathVariable String email,
                                 @ModelAttribute("employee") EmployeeDTO employeeDTO) {

        String phone = employeeDTO.getPhone();
        if (phone != null && !phone.isBlank()) {
            if (!phone.matches("^\\+?[0-9\\-\\s\\(\\)]+$")) {
                throw new InvalidProfileDataException("Помилка: Телефон не може містити літери чи спецсимволи. Дозволені лише цифри, +, -, () та пробіли.");
            }
            long digitCount = phone.chars().filter(Character::isDigit).count();
            if (digitCount < 9 || digitCount > 15) {
                throw new InvalidProfileDataException("Помилка: Номер телефону має містити від 9 до 15 цифр.");
            }
        }

        LocalDate birthDate = employeeDTO.getBirthDate();
        if (birthDate != null) {
            if (birthDate.isAfter(LocalDate.now())) {
                throw new InvalidProfileDataException("Помилка: Дата народження не може бути у майбутньому.");
            }
            if (birthDate.isBefore(LocalDate.now().minusYears(120))) {
                throw new InvalidProfileDataException("Помилка: Вказано занадто стару дату. Перевірте правильність року.");
            }
        }

        employeeService.updatePersonalData(email, phone, birthDate);

        return "redirect:/employees/" + email;
    }

    @PostMapping("/delete")
    public String deleteEmployee(@RequestParam String email, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal != null && principal.getName().equals(email)) {
            log.warn(" Спроба самовидалення: працівник {} намагався видалити свій акаунт", email);

            redirectAttributes.addFlashAttribute("errorMessage", "Ви не можете видалити власний обліковий запис.");
            return "redirect:/employees";
        }

        employeeService.deleteEmployeeByEmail(email);
        log.info(" Акаунт працівника {} було успішно видалено адміністратором/колегою {}", email, principal != null ? principal.getName() : "Система");
        return "redirect:/employees";
    }
}