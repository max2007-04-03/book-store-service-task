package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.BookDTO;
import com.epam.rd.autocode.spring.project.security.JwtUtil;
import com.epam.rd.autocode.spring.project.service.BookService;
import com.epam.rd.autocode.spring.project.service.CartService;
import com.epam.rd.autocode.spring.project.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false) // Вимикаємо фільтри безпеки для простоти тесту
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookService bookService;

    @MockBean
    private CartService cartService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    @WithMockUser(username = "test@user.com")
    void testViewCart() throws Exception {
        when(cartService.getCartItems("test@user.com")).thenReturn(Collections.emptyList());
        when(cartService.getCartTotal("test@user.com")).thenReturn(BigDecimal.ZERO);

        // Use .principal() to ensure the controller sees the user
        mockMvc.perform(get("/cart").principal(() -> "test@user.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart/view"))
                .andExpect(model().attributeExists("cartItems"))
                .andExpect(model().attributeExists("cartTotal"));
    }

    @Test
    @WithMockUser(username = "test@user.com")
    void testAddToCartSuccess() throws Exception {
        BookDTO mockBook = new BookDTO();
        mockBook.setName("Harry Potter");
        mockBook.setPrice(BigDecimal.valueOf(100));
        mockBook.setStockQuantity(10);

        when(bookService.getBookByName("Harry Potter")).thenReturn(mockBook);

        mockMvc.perform(post("/cart/add")
                        .principal(() -> "test@user.com")
                        .param("bookName", "Harry Potter")
                        .param("quantity", "2")
                        .header("Referer", "/books"))
                .andExpect(status().is3xxRedirection())
                // FIX: Use redirectedUrl for the exact match
                .andExpect(redirectedUrl("/books?added_to_cart"));

        verify(cartService).addItemToDatabaseCart(eq("test@user.com"), eq("Harry Potter"), eq(2), any(BigDecimal.class));
    }

    @Test
    @WithMockUser(username = "test@user.com")
    void testAddToCartOutOfStock() throws Exception {
        BookDTO mockBook = new BookDTO();
        mockBook.setName("Harry Potter");
        mockBook.setStockQuantity(1); // Менше, ніж ми просимо

        when(bookService.getBookByName("Harry Potter")).thenReturn(mockBook);

        mockMvc.perform(post("/cart/add")
                        .param("bookName", "Harry Potter")
                        .param("quantity", "2")
                        .header("Referer", "/books"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("errorMessage")); // Перевіряємо Flash Attribute

        // Перевіряємо, що в базу нічого не додалося
        verify(cartService, never()).addItemToDatabaseCart(anyString(), anyString(), anyInt(), any());
    }
}