package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.BookDTO;
import com.epam.rd.autocode.spring.project.security.jwt.JwtUtil;
import com.epam.rd.autocode.spring.project.service.BookService;
import com.epam.rd.autocode.spring.project.service.impl.CartService;
import com.epam.rd.autocode.spring.project.security.CustomUserDetailsService;
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
@AutoConfigureMockMvc(addFilters = false)
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
    void testViewCartWithPrincipal() throws Exception {
        when(cartService.getCartItems("test@user.com")).thenReturn(Collections.emptyList());
        when(cartService.getCartTotal("test@user.com")).thenReturn(BigDecimal.ZERO);

        mockMvc.perform(get("/cart").principal(() -> "test@user.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart/view"))
                .andExpect(model().attributeExists("cartItems"))
                .andExpect(model().attributeExists("cartTotal"));
    }

    @Test
    void testViewCartWithoutPrincipal() throws Exception {
        mockMvc.perform(get("/cart"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart/view"))
                .andExpect(model().attributeDoesNotExist("cartItems"))
                .andExpect(model().attributeDoesNotExist("cartTotal"));
    }

    @Test
    @WithMockUser(username = "test@user.com")
    void testAddToCartSuccess_WithCleanReferer() throws Exception {
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
                .andExpect(redirectedUrl("/books?added_to_cart"));

        verify(cartService).addItemToDatabaseCart(eq("test@user.com"), eq("Harry Potter"), eq(2), any(BigDecimal.class));
    }

    @Test
    @WithMockUser(username = "test@user.com")
    void testAddToCartSuccess_WithNullReferer() throws Exception {
        BookDTO mockBook = new BookDTO();
        mockBook.setName("Harry Potter");
        mockBook.setPrice(BigDecimal.valueOf(100));
        mockBook.setStockQuantity(10);

        when(bookService.getBookByName("Harry Potter")).thenReturn(mockBook);

        mockMvc.perform(post("/cart/add")
                        .principal(() -> "test@user.com")
                        .param("bookName", "Harry Potter")
                        .param("quantity", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books?added_to_cart"));
    }

    @Test
    @WithMockUser(username = "test@user.com")
    void testAddToCartSuccess_WhenRefererAlreadyContainsParam() throws Exception {
        BookDTO mockBook = new BookDTO();
        mockBook.setName("Harry Potter");
        mockBook.setPrice(BigDecimal.valueOf(100));
        mockBook.setStockQuantity(10);

        when(bookService.getBookByName("Harry Potter")).thenReturn(mockBook);

        mockMvc.perform(post("/cart/add")
                        .principal(() -> "test@user.com")
                        .param("bookName", "Harry Potter")
                        .param("quantity", "2")
                        .header("Referer", "/books?added_to_cart"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books?added_to_cart"));
    }

    @Test
    @WithMockUser(username = "test@user.com")
    void testAddToCartSuccess_WhenRefererContainsOtherParams() throws Exception {
        BookDTO mockBook = new BookDTO();
        mockBook.setName("Harry Potter");
        mockBook.setPrice(BigDecimal.valueOf(100));
        mockBook.setStockQuantity(10);

        when(bookService.getBookByName("Harry Potter")).thenReturn(mockBook);

        mockMvc.perform(post("/cart/add")
                        .principal(() -> "test@user.com")
                        .param("bookName", "Harry Potter")
                        .param("quantity", "2")
                        .header("Referer", "/books?page=2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books?page=2&added_to_cart"));
    }

    @Test
    @WithMockUser(username = "test@user.com")
    void testAddToCart_OutOfStock_LessQuantity() throws Exception {
        BookDTO mockBook = new BookDTO();
        mockBook.setName("Harry Potter");
        mockBook.setStockQuantity(1);
        when(bookService.getBookByName("Harry Potter")).thenReturn(mockBook);

        mockMvc.perform(post("/cart/add")
                        .principal(() -> "test@user.com")
                        .param("bookName", "Harry Potter")
                        .param("quantity", "2")
                        .header("Referer", "/books"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(cartService, never()).addItemToDatabaseCart(anyString(), anyString(), anyInt(), any());
    }

    @Test
    @WithMockUser(username = "test@user.com")
    void testAddToCart_OutOfStock_NullStock_WithNullReferer() throws Exception {
        BookDTO mockBook = new BookDTO();
        mockBook.setName("Harry Potter");
        mockBook.setStockQuantity(null);
        when(bookService.getBookByName("Harry Potter")).thenReturn(mockBook);

        mockMvc.perform(post("/cart/add")
                        .principal(() -> "test@user.com")
                        .param("bookName", "Harry Potter")
                        .param("quantity", "2")) // Без Referer
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(cartService, never()).addItemToDatabaseCart(anyString(), anyString(), anyInt(), any());
    }

    @Test
    @WithMockUser(username = "test@user.com")
    void testRemoveFromCart() throws Exception {
        mockMvc.perform(post("/cart/remove")
                        .principal(() -> "test@user.com")
                        .param("bookName", "Harry Potter"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));

        verify(cartService, times(1)).removeItemFromDatabaseCart("test@user.com", "Harry Potter");
    }
}