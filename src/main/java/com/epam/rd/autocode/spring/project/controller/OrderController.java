package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/client/{email}")
    public String getOrdersByClient(@PathVariable String email, Model model) {
        model.addAttribute("orders", orderService.getOrdersByClient(email));
        model.addAttribute("role", "Client");
        return "orders/list";
    }

    @GetMapping("/employee/{email}")
    public String getOrdersByEmployee(@PathVariable String email, Model model) {
        model.addAttribute("orders", orderService.getOrdersByEmployee(email));
        model.addAttribute("role", "Employee");
        return "orders/list";
    }
}