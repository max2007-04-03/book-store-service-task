package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.BookDTO;
import com.epam.rd.autocode.spring.project.dto.OrderDTO;
import com.epam.rd.autocode.spring.project.security.jwt.JwtUtil;
import com.epam.rd.autocode.spring.project.service.BookService;
import com.epam.rd.autocode.spring.project.security.CustomUserDetailsService;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENT")
    void getOrdersByClient_ShouldReturnListView() throws Exception {
        String email = "client@test.com";
        List<OrderDTO> orderList = new ArrayList<>();
        Page<OrderDTO> orderPage = new PageImpl<>(orderList);

        when(orderService.getOrdersByClient(eq(email), any(Pageable.class))).thenReturn(orderPage);

        mockMvc.perform(get("/orders/client/" + email))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/list"))
                .andExpect(model().attribute("role", "Client"))
                .andExpect(model().attributeExists("orderPage", "currentPage", "totalPages"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getOrdersByEmployee_ShouldReturnListView_WithDefaultSortDir() throws Exception {
        String email = "admin@store.com";
        List<OrderDTO> orderList = new ArrayList<>();
        Page<OrderDTO> orderPage = new PageImpl<>(orderList);

        when(orderService.getAllOrders(any(), any(Pageable.class))).thenReturn(orderPage);

        mockMvc.perform(get("/orders/employee/" + email))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/list"))
                .andExpect(model().attributeExists("orderPage", "currentPage", "totalPages", "sortBy", "sortDir"))
                .andExpect(model().attribute("sortDir", "DESC"))
                .andExpect(model().attribute("reverseSortDir", "ASC"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getOrdersByEmployee_ShouldReturnListView_WithAscSortDir() throws Exception {
        String email = "admin@store.com";
        Page<OrderDTO> orderPage = new PageImpl<>(new ArrayList<>());

        when(orderService.getAllOrders(any(), any(Pageable.class))).thenReturn(orderPage);

        mockMvc.perform(get("/orders/employee/" + email)
                        .param("sortDir", "ASC"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/list"))
                .andExpect(model().attribute("sortDir", "ASC"))
                .andExpect(model().attribute("reverseSortDir", "DESC"));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENT")
    void createOrder_ShouldRedirectToClientOrders() throws Exception {
        BookDTO book = new BookDTO();
        book.setPrice(BigDecimal.valueOf(250.0));
        when(bookService.getBookByName("Test Book")).thenReturn(book);

        mockMvc.perform(post("/orders/buy")
                        .with(csrf())
                        .param("bookName", "Test Book"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/client/client@test.com"));

        verify(orderService, times(1)).addOrder(any(OrderDTO.class));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENT")
    void checkout_ShouldRedirectToProfile_OnSuccess() throws Exception {
        doNothing().when(orderService).processCheckout("client@test.com");

        mockMvc.perform(post("/orders/checkout")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?orderSuccess=true"));

        verify(orderService, times(1)).processCheckout("client@test.com");
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENT")
    void checkout_ShouldRedirectToCartAndShowError_OnFailure() throws Exception {
        doThrow(new RuntimeException("Недостатньо коштів на балансі!"))
                .when(orderService).processCheckout("client@test.com");

        mockMvc.perform(post("/orders/checkout")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute("error", "Недостатньо коштів на балансі!"));

        verify(orderService, times(1)).processCheckout("client@test.com");
    }

    @Test
    @WithMockUser(username = "admin@store.com", roles = "EMPLOYEE")
    void shipOrder_ShouldRedirectToEmployeeOrders() throws Exception {
        doNothing().when(orderService).shipOrder(100L, "admin@store.com");

        mockMvc.perform(post("/orders/100/ship")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/employee/admin@store.com"));

        verify(orderService, times(1)).shipOrder(100L, "admin@store.com");
    }
}