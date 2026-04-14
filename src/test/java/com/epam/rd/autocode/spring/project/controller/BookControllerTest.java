package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.BookDTO;
import com.epam.rd.autocode.spring.project.exception.InvalidBookDataException;
import com.epam.rd.autocode.spring.project.security.jwt.JwtUtil;
import com.epam.rd.autocode.spring.project.security.jwt.JwtFilter;
import com.epam.rd.autocode.spring.project.service.BookService;
import com.epam.rd.autocode.spring.project.security.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = BookController.class,
        excludeAutoConfiguration = {ThymeleafAutoConfiguration.class}
)
@AutoConfigureMockMvc(addFilters = false)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookService bookService;
    private BookDTO validBookDTO;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtFilter jwtFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setUp() {
        validBookDTO = new BookDTO();
        validBookDTO.setName("Test Book");
        validBookDTO.setAuthor("Test Author");
        validBookDTO.setPrice(BigDecimal.valueOf(150.00));
        validBookDTO.setPages(300);
        validBookDTO.setStockQuantity(10);
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void listBooks_ShouldReturnListView_WithDefaultSort() throws Exception {
        Page<BookDTO> bookPage = new PageImpl<>(List.of(validBookDTO));
        when(bookService.getFilteredAndSortedBooks(any(), any(), any(), any())).thenReturn(bookPage);
        when(bookService.getAllUniqueGenres()).thenReturn(List.of("Fantasy"));

        mockMvc.perform(get("/books").param("sortBy", "name"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/list"))
                .andExpect(model().attributeExists("bookPage", "books"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void listBooks_ShouldReturnListView_WithPriceAscSort() throws Exception {
        Page<BookDTO> bookPage = new PageImpl<>(List.of(validBookDTO));
        when(bookService.getFilteredAndSortedBooks(any(), any(), any(), any())).thenReturn(bookPage);

        mockMvc.perform(get("/books").param("sortBy", "price_asc"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/list"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void listBooks_ShouldReturnListView_WithPriceDescSort() throws Exception {
        Page<BookDTO> bookPage = new PageImpl<>(List.of(validBookDTO));
        when(bookService.getFilteredAndSortedBooks(any(), any(), any(), any())).thenReturn(bookPage);

        mockMvc.perform(get("/books").param("sortBy", "price_desc"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/list"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void showCreateForm_ShouldReturnCreateView() throws Exception {
        mockMvc.perform(get("/books/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/create"))
                .andExpect(model().attribute("isEdit", false));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void showEditForm_ShouldReturnEditView() throws Exception {
        when(bookService.getBookByName("Test Book")).thenReturn(validBookDTO);

        mockMvc.perform(get("/books/Test Book/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/create"))
                .andExpect(model().attribute("isEdit", true))
                .andExpect(model().attributeExists("book"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void addBook_ShouldRedirectToBooksList_WhenDataIsValid() throws Exception {
        when(bookService.addBook(any(BookDTO.class))).thenReturn(validBookDTO);

        mockMvc.perform(post("/books")
                        .with(csrf())
                        .param("name", "Test Book")
                        .param("author", "Test Author")
                        .param("price", "150.00")
                        .param("pages", "100")
                        .param("stockQuantity", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books"));

        verify(bookService, times(1)).addBook(any(BookDTO.class));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void addBook_ShouldThrowException_WhenPriceIsNegative() throws Exception {
        mockMvc.perform(post("/books").with(csrf()).param("price", "-10.00"))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof InvalidBookDataException));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void addBook_ShouldThrowException_WhenPagesLessThanOne() throws Exception {
        mockMvc.perform(post("/books").with(csrf()).param("pages", "0"))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof InvalidBookDataException));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void addBook_ShouldThrowException_WhenStockQuantityIsNegative() throws Exception {
        mockMvc.perform(post("/books").with(csrf()).param("stockQuantity", "-5"))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof InvalidBookDataException));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void addBook_ShouldThrowException_WhenBindingResultHasErrors() throws Exception {
        mockMvc.perform(post("/books").with(csrf()).param("price", "INVALID_PRICE"))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof InvalidBookDataException));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void updateBook_ShouldRedirectToBooksList_WhenDataIsValid() throws Exception {
        when(bookService.updateBookByName(eq("Test Book"), any(BookDTO.class))).thenReturn(validBookDTO);

        mockMvc.perform(post("/books/Test Book/update")
                        .with(csrf())
                        .param("name", "Test Book")
                        .param("author", "Test Author")
                        .param("price", "200.00")
                        .param("pages", "150")
                        .param("stockQuantity", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books"));

        verify(bookService, times(1)).updateBookByName(eq("Test Book"), any(BookDTO.class));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void updateBook_ShouldThrowException_WhenPriceIsNegative() throws Exception {
        mockMvc.perform(post("/books/Test Book/update").with(csrf()).param("price", "-5.00"))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof InvalidBookDataException));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void updateBook_ShouldThrowException_WhenPagesLessThanOne() throws Exception {
        mockMvc.perform(post("/books/Test Book/update").with(csrf()).param("pages", "-10"))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof InvalidBookDataException));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void updateBook_ShouldThrowException_WhenStockQuantityIsNegative() throws Exception {
        mockMvc.perform(post("/books/Test Book/update").with(csrf()).param("stockQuantity", "-1"))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof InvalidBookDataException));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void updateBook_ShouldThrowException_WhenBindingResultHasErrors() throws Exception {
        mockMvc.perform(post("/books/Test Book/update").with(csrf()).param("pages", "NOT_A_NUMBER"))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof InvalidBookDataException));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void deleteBook_ShouldRedirectToBooksList() throws Exception {
        doNothing().when(bookService).deleteBookByName("Test Book");

        mockMvc.perform(post("/books/Test Book/delete")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books"));

        verify(bookService, times(1)).deleteBookByName("Test Book");
    }
}