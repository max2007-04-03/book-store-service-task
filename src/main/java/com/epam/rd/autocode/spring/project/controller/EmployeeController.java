package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.EmployeeDTO;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

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
    public String updateEmployeeProfile(@PathVariable String email,
                                        @Valid @ModelAttribute("employee") EmployeeDTO employeeDTO,
                                        BindingResult bindingResult,
                                        Principal principal,
                                        Model model) {

        if (principal == null || !principal.getName().equals(email)) {
            log.warn("❌ Безпека: {} намагається оновити дані чужого профілю ({})",
                    principal != null ? principal.getName() : "Гість", email);
            return "redirect:/employees";
        }

        if (bindingResult.hasErrors()) {
            log.warn("❌ Помилка валідації при оновленні профілю {}: {}", email, bindingResult.getAllErrors());
            return "employees/employee-edit";
        }

        employeeService.updatePersonalData(email, employeeDTO.getPhone(), employeeDTO.getBirthDate());
        log.info("✅ Працівник {} успішно оновив свої персональні дані", email);

        return "redirect:/employees/" + email;
    }

    @PostMapping("/delete")
    public String deleteEmployee(@RequestParam String email, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal != null && principal.getName().equals(email)) {
            log.warn("❌ Спроба самовидалення: працівник {} намагався видалити свій акаунт", email);

            redirectAttributes.addFlashAttribute("errorMessage", "Ви не можете видалити власний обліковий запис.");
            return "redirect:/employees";
        }

        employeeService.deleteEmployeeByEmail(email);
        log.info("🗑️ Акаунт працівника {} було успішно видалено адміністратором/колегою {}", email, principal != null ? principal.getName() : "Система");
        return "redirect:/employees";
    }
}