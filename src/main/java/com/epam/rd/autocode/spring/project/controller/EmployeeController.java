package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.EmployeeDTO;
import com.epam.rd.autocode.spring.project.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public String getAllEmployees(Model model) {
        model.addAttribute("employees", employeeService.getAllEmployees());
        return "employees/list";
    }

    @GetMapping("/{email}")
    public String getEmployeeDetails(@PathVariable String email, Model model) {
        model.addAttribute("employee", employeeService.getEmployeeByEmail(email));
        return "employees/detail";
    }

    @PostMapping("/delete")
    public String deleteEmployee(@RequestParam String email) {
        employeeService.deleteEmployeeByEmail(email);
        return "redirect:/employees";
    }
}