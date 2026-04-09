package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.ClientDTO;
import com.epam.rd.autocode.spring.project.repo.ClientRepository;
import com.epam.rd.autocode.spring.project.repo.EmployeeRepository;
import com.epam.rd.autocode.spring.project.security.JwtUtil;
import com.epam.rd.autocode.spring.project.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import com.epam.rd.autocode.spring.project.service.OrderService;
import org.modelmapper.ModelMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Вимикаємо Security для чистих тестів контролера
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;


    @MockBean
    private com.epam.rd.autocode.spring.project.security.JwtFilter jwtFilter;
    @MockBean
    private AuthenticationManager authenticationManager;
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private PasswordEncoder passwordEncoder;
    @MockBean
    private ClientService clientService;
    @MockBean
    private EmployeeService employeeService;
    @MockBean
    private CustomUserDetailsService customUserDetailsService;
    @MockBean
    private BookService bookService;
    @MockBean
    private CartService cartService;
    @MockBean
    private OrderService orderService;
    @MockBean
    private ModelMapper modelMapper;
    @MockBean
    private ClientRepository clientRepository;
    @MockBean
    private EmployeeRepository employeeRepository;



    @Test
    void testShowLoginForm() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void testProcessLoginSuccess() throws Exception {
        // Налаштовуємо мок для генерації токена
        when(jwtUtil.generateToken("slyvka@gmail.com")).thenReturn("fake-jwt-token");

        mockMvc.perform(post("/login")
                        .param("username", "slyvka@gmail.com")
                        .param("password", "password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"))
                .andExpect(cookie().exists("JWT"))
                .andExpect(cookie().value("JWT", "fake-jwt-token"));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void testProcessRegistrationClient() throws Exception {
        mockMvc.perform(post("/register")
                        .param("name", "Test User")
                        .param("email", "test@gmail.com")
                        .param("password", "Secure123") // Виправлено: складний пароль
                        .param("balance", "100.00")    // Додано: обов'язкове поле
                        .param("role", "CLIENT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        verify(clientService, times(1)).addClient(any(ClientDTO.class));
    }

    @Test
    void testProcessRegistrationEmployeeWrongCode() throws Exception {
        mockMvc.perform(post("/register")
                        .param("name", "Employee")
                        .param("email", "emp@gmail.com")
                        .param("password", "Secure123") // Виправлено: складний пароль
                        .param("balance", "0.00")      // Додано: обов'язкове поле
                        .param("role", "EMPLOYEE")
                        .param("serviceCode", "WRONG-CODE"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error")); // Тепер атрибут з'явиться!

        verify(employeeService, never()).addEmployee(any());
    }
}