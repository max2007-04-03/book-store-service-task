package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.ClientDTO;
import com.epam.rd.autocode.spring.project.security.JwtUtil;
import com.epam.rd.autocode.spring.project.service.CartService;
import com.epam.rd.autocode.spring.project.service.ClientService;
import com.epam.rd.autocode.spring.project.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// 1. Відключаємо Thymeleaf для тестів контролера
@WebMvcTest(controllers = ClientController.class, excludeAutoConfiguration = {ThymeleafAutoConfiguration.class})
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClientService clientService;

    // 2. Додаємо Mock-заглушки для Security/JWT
    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private CartService cartService;


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
                // 3. Перевіряємо атрибути, які твій контролер реально додає в модель
                .andExpect(model().attributeExists("clientPage", "clients", "currentPage", "totalPages"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getClientDetails_ShouldReturnDetailView() throws Exception {
        String email = "test@client.com";
        when(clientService.getClientByEmail(email)).thenReturn(new ClientDTO());

        mockMvc.perform(get("/clients/" + email))
                .andExpect(status().isOk())
                // ВИПРАВЛЕНО: тепер очікується правильний шлях з папкою
                .andExpect(view().name("clients/detail"))
                .andExpect(model().attributeExists("client"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void deleteClient_ShouldRedirect() throws Exception {
        String email = "delete@client.com";
        doNothing().when(clientService).deleteClientByEmail(email);

        // 3. Додаємо .with(csrf()) до POST-запиту
        mockMvc.perform(post("/clients/delete")
                        .with(csrf())
                        .param("email", email))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clients"));

        verify(clientService, times(1)).deleteClientByEmail(email);
    }


}