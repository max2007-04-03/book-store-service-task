package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.OrderDTO;
import com.epam.rd.autocode.spring.project.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final OrderService orderService;

    @GetMapping({"/", "/home"})
    public String home(Model model, Principal principal) {
        log.debug(" Запит головної сторінки (Home)");

        if (principal != null) {
            String email = principal.getName();
            List<OrderDTO> newNotifications = orderService.getNewShippedOrders(email);
            model.addAttribute("orders", newNotifications);

            if (!newNotifications.isEmpty()) {
                log.info(" Користувач {} має {} нових сповіщень про відправку замовлень", email, newNotifications.size());
            }
        }

        return "home";
    }
}