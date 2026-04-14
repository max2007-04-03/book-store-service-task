package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.ClientDTO;
import com.epam.rd.autocode.spring.project.dto.EmployeeDTO;
import com.epam.rd.autocode.spring.project.repo.ClientRepository;
import com.epam.rd.autocode.spring.project.repo.EmployeeRepository;
import com.epam.rd.autocode.spring.project.security.jwt.JwtUtil;
import com.epam.rd.autocode.spring.project.security.jwt.JwtFilter;
import com.epam.rd.autocode.spring.project.service.*;
import com.epam.rd.autocode.spring.project.service.impl.CartService;
import com.epam.rd.autocode.spring.project.security.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private JwtFilter jwtFilter;
    @MockBean private AuthenticationManager authenticationManager;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private PasswordEncoder passwordEncoder;
    @MockBean private ClientService clientService;
    @MockBean private EmployeeService employeeService;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private BookService bookService;
    @MockBean private CartService cartService;
    @MockBean private OrderService orderService;
    @MockBean private ModelMapper modelMapper;
    @MockBean private ClientRepository clientRepository;
    @MockBean private EmployeeRepository employeeRepository;


    @Test
    void testShowLoginForm() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void testProcessLoginSuccess() throws Exception {
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
    void testProcessLoginBadCredentials() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad creds"));

        mockMvc.perform(post("/login").param("username", "user").param("password", "pass"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("error", "Невірний пароль"));
    }

    @Test
    void testProcessLoginDisabled() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new DisabledException("Disabled account"));

        mockMvc.perform(post("/login").param("username", "user").param("password", "pass"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("error", "Ваш акаунт заблоковано"));
    }

    @Test
    void testProcessLoginLocked() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new LockedException("Locked account"));

        mockMvc.perform(post("/login").param("username", "user").param("password", "pass"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("error", "Акаунт заблоковано через кілька невдалих спроб"));
    }

    @Test
    void testProcessLoginAuthenticationException() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new AccountExpiredException("Expired"));

        mockMvc.perform(post("/login").param("username", "user").param("password", "pass"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("error", "Помилка входу: Expired"));
    }

    @Test
    void testProcessLoginGeneralException() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new RuntimeException("Some internal error"));

        mockMvc.perform(post("/login").param("username", "user").param("password", "pass"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("error", "Сталася внутрішня помилка сервера"));
    }

    @Test
    void testShowRegisterForm() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    void testProcessRegistrationClient() throws Exception {
        mockMvc.perform(post("/register")
                        .param("name", "Test User")
                        .param("email", "test@gmail.com")
                        .param("password", "Secure123")
                        .param("balance", "100.00")
                        .param("role", "CLIENT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        verify(clientService, times(1)).addClient(any(ClientDTO.class));
    }

    @Test
    void testProcessRegistrationNullBalance() throws Exception {
        mockMvc.perform(post("/register")
                        .param("name", "Test User")
                        .param("email", "test@gmail.com")
                        .param("password", "Secure123")
                        .param("role", "CLIENT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        ArgumentCaptor<ClientDTO> captor = ArgumentCaptor.forClass(ClientDTO.class);
        verify(clientService).addClient(captor.capture());
        assertEquals(BigDecimal.ZERO, captor.getValue().getBalance());
    }

    @Test
    void testProcessRegistrationValidationError() throws Exception {
        mockMvc.perform(post("/register")
                        .param("name", "Test User")
                        .param("email", "test@gmail.com")
                        .param("password", "Secure123")
                        .param("balance", "INVALID_NUMBER")
                        .param("role", "CLIENT"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));

        verify(clientService, never()).addClient(any());
    }

    @Test
    void testProcessRegistrationEmployeeSuccess() throws Exception {
        mockMvc.perform(post("/register")
                        .param("name", "Employee")
                        .param("email", "emp@gmail.com")
                        .param("password", "Secure123")
                        .param("balance", "0.00")
                        .param("role", "EMPLOYEE")
                        .param("serviceCode", "WORK-2026"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        ArgumentCaptor<EmployeeDTO> captor = ArgumentCaptor.forClass(EmployeeDTO.class);
        verify(employeeService, times(1)).addEmployee(captor.capture());
        assertEquals("emp@gmail.com", captor.getValue().getEmail());
    }

    @Test
    void testProcessRegistrationEmployeeWrongCode() throws Exception {
        mockMvc.perform(post("/register")
                        .param("name", "Employee")
                        .param("email", "emp@gmail.com")
                        .param("password", "Secure123")
                        .param("balance", "0.00")
                        .param("role", "EMPLOYEE")
                        .param("serviceCode", "WRONG-CODE"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));

        verify(employeeService, never()).addEmployee(any());
    }

    @Test
    void testLogout() throws Exception {
        mockMvc.perform(post("/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"))
                .andExpect(cookie().maxAge("JWT", 0));
    }
}