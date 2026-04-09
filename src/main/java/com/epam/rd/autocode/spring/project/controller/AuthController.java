package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.ClientDTO;
import com.epam.rd.autocode.spring.project.dto.EmployeeDTO;
import com.epam.rd.autocode.spring.project.security.JwtUtil;
import com.epam.rd.autocode.spring.project.service.ClientService;
import com.epam.rd.autocode.spring.project.service.EmployeeService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final ClientService clientService;
    private final EmployeeService employeeService;

    private static final String JWT_COOKIE_NAME = "JWT";
    private static final String EMPLOYEE_SECRET = "WORK-2026";
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";
    private static final String ROLE_CLIENT = "CLIENT";

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam String username,
                               @RequestParam String password,
                               HttpServletResponse response,
                               Model model) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));

            String token = jwtUtil.generateToken(username);

            Cookie jwtCookie = new Cookie(JWT_COOKIE_NAME, token);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(60 * 60 * 10);
            response.addCookie(jwtCookie);

            log.info(" Користувач {} успішно увійшов у систему", username);
            return "redirect:/home";

        } catch (BadCredentialsException e) {
            log.warn(" Невірний пароль для: {}", username);
            model.addAttribute("error", "Невірний пароль");
            return "login";

        } catch (DisabledException e) {
            log.warn(" Акаунт заблоковано/деактивовано: {}", username);
            model.addAttribute("error", "Ваш акаунт заблоковано");
            return "login";

        } catch (LockedException e) {
            log.warn(" Акаунт користувача {} заблоковано", username);
            model.addAttribute("error", "Акаунт заблоковано через кілька невдалих спроб");
            return "login";

        } catch (AuthenticationException e) {
            log.warn(" Невдала спроба входу для: {}. Причина: {}", username, e.getMessage());
            model.addAttribute("error", "Помилка входу: " + e.getMessage());
            return "login";

        } catch (Exception e) {
            log.error(" Непередбачена помилка при вході: ", e);
            model.addAttribute("error", "Сталася внутрішня помилка сервера");
            return "login";
        }


    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("user", new ClientDTO());
        return "register";
    }

    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("user") ClientDTO clientDTO,
                                      BindingResult bindingResult,
                                      @RequestParam String role,
                                      @RequestParam(required = false) String serviceCode,
                                      Model model) {

        if (clientDTO.getBalance() == null) {
            clientDTO.setBalance(java.math.BigDecimal.ZERO);
        }

        if (bindingResult.hasErrors()) {
            log.error("Помилки валідації: {}", bindingResult.getAllErrors());
            return "register";
        }

        String rawPassword = clientDTO.getPassword();
        String encodedPassword = passwordEncoder.encode(rawPassword);
        clientDTO.setPassword(encodedPassword);

        if (ROLE_EMPLOYEE.equals(role)) {
            if (!EMPLOYEE_SECRET.equals(serviceCode)) {
                model.addAttribute("error", "Невірний службовий код реєстрації!");
                return "register";
            }

            EmployeeDTO empDto = new EmployeeDTO();
            empDto.setEmail(clientDTO.getEmail());
            empDto.setName(clientDTO.getName());
            empDto.setPassword(encodedPassword);
            employeeService.addEmployee(empDto);
            log.info("✅ Зареєстровано нового працівника: {}", empDto.getEmail());

        } else if (ROLE_CLIENT.equals(role)) {
            clientService.addClient(clientDTO);
            log.info("✅ Зареєстровано нового клієнта: {}", clientDTO.getEmail());
        }

        return "redirect:/login?registered";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        Cookie jwtCookie = new Cookie("JWT", null);
        jwtCookie.setPath("/");
        jwtCookie.setHttpOnly(true);
        jwtCookie.setMaxAge(0);
        response.addCookie(jwtCookie);

        SecurityContextHolder.clearContext();

        log.info("✅ Користувач успішно вийшов із системи");
        return "redirect:/login?logout";
    }
}