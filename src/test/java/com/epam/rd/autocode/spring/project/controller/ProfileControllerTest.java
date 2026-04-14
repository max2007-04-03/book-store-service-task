package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.ClientDTO;
import com.epam.rd.autocode.spring.project.exception.InvalidProfileDataException;
import com.epam.rd.autocode.spring.project.security.jwt.JwtUtil;
import com.epam.rd.autocode.spring.project.service.*;
import com.epam.rd.autocode.spring.project.repo.ClientRepository;
import com.epam.rd.autocode.spring.project.repo.EmployeeRepository;
import com.epam.rd.autocode.spring.project.service.impl.CartService;
import com.epam.rd.autocode.spring.project.security.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProfileController profileController;

    @MockBean
    private ClientService clientService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtUtil jwtUtil;

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
    void testUpdateProfile_EmailChanged() throws Exception {
        mockMvc.perform(post("/profile/update")
                        .principal(() -> "test@gmail.com")
                        .param("email", "new@gmail.com")
                        .param("name", "New Name"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?emailChanged"))
                .andExpect(cookie().maxAge("JWT", 0));

        verify(clientService).updateClientProfile(eq("test@gmail.com"), any(ClientDTO.class));
    }

    @Test
    void testUpdateProfile_EmailNotChanged() throws Exception {
        mockMvc.perform(post("/profile/update")
                        .principal(() -> "test@gmail.com")
                        .param("email", "test@gmail.com")
                        .param("name", "Same Name"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?success"))
                .andExpect(cookie().doesNotExist("JWT"));

        verify(clientService).updateClientProfile(eq("test@gmail.com"), any(ClientDTO.class));
    }

    @Test
    void testUpdateProfile_ShouldThrowException_WhenNameIsNull() throws Exception {
        mockMvc.perform(post("/profile/update")
                        .principal(() -> "test@gmail.com")
                        .param("email", "test@gmail.com"))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof InvalidProfileDataException));

        verify(clientService, never()).updateClientProfile(anyString(), any(ClientDTO.class));
    }

    @Test
    void testUpdateProfile_ShouldThrowException_WhenNameIsEmptyOrBlank() throws Exception {
        mockMvc.perform(post("/profile/update")
                        .principal(() -> "test@gmail.com")
                        .param("email", "test@gmail.com")
                        .param("name", "   "))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof InvalidProfileDataException));

        verify(clientService, never()).updateClientProfile(anyString(), any(ClientDTO.class));
    }

    @Test
    void testTopUpBalance_Success() throws Exception {
        mockMvc.perform(post("/profile/topup")
                        .principal(() -> "test@gmail.com")
                        .param("amount", "500.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?topupSuccess=true"));

        verify(clientService).addBalance("test@gmail.com", new BigDecimal("500.00"));
    }

    @Test
    void testTopUpBalance_ShouldRedirectWithError_WhenAmountIsZero() throws Exception {
        mockMvc.perform(post("/profile/topup")
                        .principal(() -> "test@gmail.com")
                        .param("amount", "0.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("error"));

        verify(clientService, never()).addBalance(anyString(), any(BigDecimal.class));
    }

    @Test
    void testTopUpBalance_ShouldRedirectWithError_WhenAmountIsNegative() throws Exception {
        mockMvc.perform(post("/profile/topup")
                        .principal(() -> "test@gmail.com")
                        .param("amount", "-100.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("error"));

        verify(clientService, never()).addBalance(anyString(), any(BigDecimal.class));
    }

    @Test
    void testTopUpBalance_ShouldRedirectWithError_WhenAmountIsNull() {
        Principal mockPrincipal = mock(Principal.class);
        when(mockPrincipal.getName()).thenReturn("test@gmail.com");

        RedirectAttributes mockRedirectAttributes = mock(RedirectAttributes.class);

        String result = profileController.topUpBalance(null, mockPrincipal, mockRedirectAttributes);

        assertEquals("redirect:/profile", result);
        verify(mockRedirectAttributes).addFlashAttribute(eq("error"), anyString());
        verify(clientService, never()).addBalance(anyString(), any(BigDecimal.class));
    }
}