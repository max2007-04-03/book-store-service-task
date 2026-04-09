package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.ClientDTO;
import com.epam.rd.autocode.spring.project.service.ClientService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;

@Slf4j
@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ClientService clientService;

    @GetMapping
    public String showProfile(Model model, Principal principal) {
        ClientDTO client = clientService.getClientByEmail(principal.getName());
        model.addAttribute("client", client);
        return "client-profile";
    }

    @PostMapping("/update")
    public String updateProfile(@Valid @ModelAttribute("client") ClientDTO clientDTO,
                                BindingResult bindingResult,
                                Principal principal,
                                HttpServletResponse response,
                                Model model) {

        if (bindingResult.hasErrors()) {
            log.warn("❌ Помилка валідації при оновленні профілю {}: {}", principal.getName(), bindingResult.getAllErrors());
            return "client-profile";
        }

        boolean emailChanged = !principal.getName().equals(clientDTO.getEmail());
        clientService.updateClientProfile(principal.getName(), clientDTO);
        log.info("✅ Користувач {} успішно оновив свій профіль", principal.getName());

        if (emailChanged) {
            log.info("🔄 Користувач змінив email. Видалення JWT токена та редірект на логін.");
            Cookie cookie = new Cookie("JWT", null);
            cookie.setPath("/");
            cookie.setMaxAge(0);
            response.addCookie(cookie);
            return "redirect:/login?emailChanged";
        }

        return "redirect:/profile?success";
    }


    @PostMapping("/topup")
    public String topUpBalance(@RequestParam BigDecimal amount,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            redirectAttributes.addFlashAttribute("error", "Сума поповнення має бути більшою за нуль.");
            return "redirect:/profile";
        }

        clientService.addBalance(principal.getName(), amount);
        return "redirect:/profile?topupSuccess=true";
    }
}