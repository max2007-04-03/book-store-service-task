package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.ClientDTO;
import com.epam.rd.autocode.spring.project.security.jwt.JwtUtil;
import com.epam.rd.autocode.spring.project.service.impl.CartService;
import com.epam.rd.autocode.spring.project.service.ClientService;
import com.epam.rd.autocode.spring.project.security.CustomUserDetailsService;
import com.epam.rd.autocode.spring.project.repo.ClientRepository;
import com.epam.rd.autocode.spring.project.repo.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ClientController.class, excludeAutoConfiguration = {ThymeleafAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClientService clientService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private CartService cartService;

    @MockBean
    private ClientRepository clientRepository;

    @MockBean
    private EmployeeRepository employeeRepository;

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getAllClients_ShouldReturnListView() throws Exception {
        List<ClientDTO> clients = List.of(new ClientDTO());
        org.springframework.data.domain.Page<ClientDTO> clientPage =
                new org.springframework.data.domain.PageImpl<>(clients);

        when(clientService.getAllClients(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(clientPage);

        mockMvc.perform(get("/clients"))
                .andExpect(status().isOk())
                .andExpect(view().name("clients/list"))
                .andExpect(model().attributeExists("clientPage", "clients", "currentPage", "totalPages"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getClientDetails_ShouldReturnDetailView() throws Exception {
        String email = "test@client.com";
        when(clientService.getClientByEmail(email)).thenReturn(new ClientDTO());

        mockMvc.perform(get("/clients/" + email))
                .andExpect(status().isOk())
                .andExpect(view().name("clients/detail"))
                .andExpect(model().attributeExists("client"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void editClientForm_ShouldReturnEditView() throws Exception {
        String email = "test@client.com";
        when(clientService.getClientByEmail(email)).thenReturn(new ClientDTO());

        mockMvc.perform(get("/clients/" + email + "/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("clients/client-edit"))
                .andExpect(model().attributeExists("client"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void updateClient_ShouldRedirect_WhenDataIsValid() throws Exception {
        String email = "test@client.com";
        doNothing().when(clientService).updateClient(eq(email), any(ClientDTO.class));

        mockMvc.perform(post("/clients/" + email + "/update")
                        .with(csrf())
                        .param("name", "Updated Name")
                        .param("email", email)
                        .param("password", "SecurePass123")
                        .param("balance", "150.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clients/" + email));

        verify(clientService, times(1)).updateClient(eq(email), any(ClientDTO.class));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void updateClient_ShouldReturnEditView_WhenValidationFails() throws Exception {
        String email = "test@client.com";

        mockMvc.perform(post("/clients/" + email + "/update")
                        .with(csrf())
                        .param("balance", "INVALID_NUMBER"))
                .andExpect(status().isOk())
                .andExpect(view().name("clients/client-edit"))
                .andExpect(model().attributeExists("client"));

        verify(clientService, never()).updateClient(anyString(), any(ClientDTO.class));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void deleteClient_ShouldRedirect() throws Exception {
        String email = "delete@client.com";
        doNothing().when(clientService).deleteClientByEmail(email);

        mockMvc.perform(post("/clients/delete")
                        .with(csrf())
                        .param("email", email))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clients"));

        verify(clientService, times(1)).deleteClientByEmail(email);
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getClientCart_ShouldReturnCartView() throws Exception {
        String email = "test@client.com";
        when(cartService.getCartItems(email)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/clients/" + email + "/cart"))
                .andExpect(status().isOk())
                .andExpect(view().name("clients/cart-view"))
                .andExpect(model().attributeExists("clientEmail", "cartItems"))
                .andExpect(model().attribute("clientEmail", email));

        verify(cartService, times(1)).getCartItems(email);
    }
}