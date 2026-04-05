package com.epam.rd.autocode.spring.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping({"/", "/home"})
    public String home() {
        return "home"; // Повертає шаблон src/main/resources/templates/home.html
    }
}