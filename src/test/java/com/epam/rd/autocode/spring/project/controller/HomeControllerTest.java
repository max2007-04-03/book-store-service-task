package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.OrderDTO;
import com.epam.rd.autocode.spring.project.security.jwt.JwtUtil;
import com.epam.rd.autocode.spring.project.security.CustomUserDetailsService;
import com.epam.rd.autocode.spring.project.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = HomeController.class, excludeAutoConfiguration = {ThymeleafAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class HomeControllerTest {

    @MockBean
    private OrderService orderService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class DummyViewConfig {
        @Bean
        public ViewResolver viewResolver() {
            InternalResourceViewResolver resolver = new InternalResourceViewResolver();
            resolver.setPrefix("/templates/");
            resolver.setSuffix(".html");
            return resolver;
        }
    }

    @Test
    void home_ShouldReturnHomeView_WhenPrincipalIsNull() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeDoesNotExist("orders"));

        verify(orderService, never()).getNewShippedOrders(anyString());
    }

    @Test
    @WithMockUser(username = "test@user.com")
    void home_ShouldReturnHomeView_WithEmptyNotifications_WhenPrincipalExists() throws Exception {
        when(orderService.getNewShippedOrders("test@user.com")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/home").principal(() -> "test@user.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("orders"))
                .andExpect(model().attribute("orders", Collections.emptyList()));

        verify(orderService, times(1)).getNewShippedOrders("test@user.com");
    }

    @Test
    @WithMockUser(username = "test@user.com")
    void home_ShouldReturnHomeView_WithNotifications_WhenPrincipalExists() throws Exception {
        List<OrderDTO> mockOrders = List.of(new OrderDTO()); // Список із 1 замовленням
        when(orderService.getNewShippedOrders("test@user.com")).thenReturn(mockOrders);

        mockMvc.perform(get("/").principal(() -> "test@user.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("orders"))
                .andExpect(model().attribute("orders", mockOrders));

        verify(orderService, times(1)).getNewShippedOrders("test@user.com");
    }
}