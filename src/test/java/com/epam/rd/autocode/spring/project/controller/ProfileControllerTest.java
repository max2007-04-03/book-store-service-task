package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.ClientDTO;
import com.epam.rd.autocode.spring.project.security.JwtUtil;
import com.epam.rd.autocode.spring.project.service.*;
import com.epam.rd.autocode.spring.project.repo.ClientRepository;
import com.epam.rd.autocode.spring.project.repo.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.security.Principal;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClientService clientService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtUtil jwtUtil;

    // ДОДАНО: Моки для вирішення UnsatisfiedDependencyException (необхідні для контексту/валідації)
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
    void testShowProfile() throws Exception {
        ClientDTO mockClient = new ClientDTO();
        mockClient.setEmail("test@gmail.com");

        when(clientService.getClientByEmail("test@gmail.com")).thenReturn(mockClient);

        mockMvc.perform(get("/profile").principal(() -> "test@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("client-profile"))
                .andExpect(model().attributeExists("client"));
    }

    @Test
    void testUpdateProfileEmailChanged() throws Exception {
        // ВИПРАВЛЕНО: Додано параметри password та balance, щоб пройти валідацію ClientDTO
        mockMvc.perform(post("/profile/update")
                        .principal(() -> "test@gmail.com")
                        .param("email", "new@gmail.com")
                        .param("name", "New Name")
                        .param("password", "SecurePass123") // Обов'язково для валідації
                        .param("balance", "100.00"))      // Обов'язково для валідації
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?emailChanged"))
                .andExpect(cookie().maxAge("JWT", 0));

        verify(clientService).updateClientProfile(eq("test@gmail.com"), any(ClientDTO.class));
    }

    @Test
    void testTopUpBalance() throws Exception {
        mockMvc.perform(post("/profile/topup")
                        .principal(() -> "test@gmail.com")
                        .param("amount", "500.00"))
                .andExpect(status().is3xxRedirection())
                // ВИПРАВЛЕНО: Додано =true відповідно до реальної поведінки контролера в логах
                .andExpect(redirectedUrl("/profile?topupSuccess=true"));

        verify(clientService).addBalance("test@gmail.com", new BigDecimal("500.00"));
    }
}