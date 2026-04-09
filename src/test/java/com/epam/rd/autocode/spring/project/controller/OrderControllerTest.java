package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.OrderDTO;
import com.epam.rd.autocode.spring.project.security.JwtUtil;
import com.epam.rd.autocode.spring.project.service.BookService;
import com.epam.rd.autocode.spring.project.service.CustomUserDetailsService;
import com.epam.rd.autocode.spring.project.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OrderController.class, excludeAutoConfiguration = {ThymeleafAutoConfiguration.class})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private BookService bookService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    // ShoppingCart ВИДАЛЕНО, бо він більше не використовується

    @Test
    @WithMockUser(roles = "CLIENT")
    void getOrdersByClient_ShouldReturnListView() throws Exception {
        String email = "client@test.com";
        // Створюємо імітацію сторінки (Page) замість списку
        List<OrderDTO> orderList = new ArrayList<>();
        Page<OrderDTO> orderPage = new PageImpl<>(orderList);

        // Передаємо два аргументи: email та будь-який Pageable
        when(orderService.getOrdersByClient(eq(email), any(Pageable.class))).thenReturn(orderPage);

        mockMvc.perform(get("/orders/client/" + email))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/list"))
                .andExpect(model().attribute("role", "Client"))
                // Перевіряємо атрибути, які додає контролер для пагінації
                .andExpect(model().attributeExists("orderPage", "currentPage", "totalPages"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getOrdersByEmployee_ShouldReturnListView() throws Exception {
        String email = "admin@store.com";
        List<OrderDTO> orderList = new ArrayList<>();
        Page<OrderDTO> orderPage = new PageImpl<>(orderList);

        // В контролері для працівника ти викликаєш getEmployeeDashboardOrders
        when(orderService.getEmployeeDashboardOrders(eq(email), any(Pageable.class))).thenReturn(orderPage);

        mockMvc.perform(get("/orders/employee/" + email))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/list"))
                .andExpect(model().attribute("role", "Employee"))
                .andExpect(model().attributeExists("orderPage", "currentPage", "totalPages"));
    }
}