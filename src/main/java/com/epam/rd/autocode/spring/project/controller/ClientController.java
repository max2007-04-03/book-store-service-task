package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.ClientDTO;
import com.epam.rd.autocode.spring.project.service.ClientService;
import com.epam.rd.autocode.spring.project.service.impl.CartService;

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

@Slf4j
@Controller
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;
    private final CartService cartService;


    @GetMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public String getAllClients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("email"));
        Page<ClientDTO> clientPage = clientService.getAllClients(pageable);

        model.addAttribute("clientPage", clientPage);
        model.addAttribute("clients", clientPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", clientPage.getTotalPages());

        return "clients/list";
    }

    @GetMapping("/{email}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'CLIENT')")
    public String getClientDetails(@PathVariable String email, Model model) {
        model.addAttribute("client", clientService.getClientByEmail(email));
        return "clients/detail";
    }

    @GetMapping("/{email}/edit")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'CLIENT')")
    public String editClientForm(@PathVariable String email, Model model) {
        model.addAttribute("client", clientService.getClientByEmail(email));
        return "clients/client-edit";
    }


    @PostMapping("/{email}/update")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'CLIENT')")
    public String updateClient(@PathVariable String email,
                               @Valid @ModelAttribute("client") ClientDTO clientDTO,
                               BindingResult bindingResult,
                               Model model) {

        if (bindingResult.hasErrors()) {
            log.warn(" Помилка валідації при оновленні профілю клієнта {}: {}", email, bindingResult.getAllErrors());
            model.addAttribute("client", clientDTO);
            return "clients/client-edit";
        }

        clientService.updateClient(email, clientDTO);
        log.info(" Успішно оновлено дані клієнта: {}", email);
        return "redirect:/clients/" + email;
    }

    @PostMapping("/delete")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public String deleteClient(@RequestParam String email) {
        clientService.deleteClientByEmail(email);
        log.info(" Працівник успішно видалив клієнта: {}", email);
        return "redirect:/clients";
    }

    @GetMapping("/{email}/cart")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public String getClientCart(@PathVariable String email, Model model) {
        log.debug("▶ Працівник переглядає кошик клієнта: {}", email);
        model.addAttribute("clientEmail", email);
        model.addAttribute("cartItems", cartService.getCartItems(email));
        return "clients/cart-view";
    }
}