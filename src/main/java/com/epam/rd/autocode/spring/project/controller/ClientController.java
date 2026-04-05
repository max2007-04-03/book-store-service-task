package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.ClientDTO;
import com.epam.rd.autocode.spring.project.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    public String getAllClients(Model model) {
        model.addAttribute("clients", clientService.getAllClients());
        return "clients/list";
    }

    @GetMapping("/{email}")
    public String getClientDetails(@PathVariable String email, Model model) {
        model.addAttribute("client", clientService.getClientByEmail(email));
        return "clients/detail";
    }

    @PostMapping("/delete")
    public String deleteClient(@RequestParam String email) {
        clientService.deleteClientByEmail(email);
        return "redirect:/clients";
    }
}