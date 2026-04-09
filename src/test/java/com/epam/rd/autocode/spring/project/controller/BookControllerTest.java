package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.BookDTO;
import com.epam.rd.autocode.spring.project.security.JwtUtil;
import com.epam.rd.autocode.spring.project.service.BookService;
import com.epam.rd.autocode.spring.project.service.CustomUserDetailsService;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;


import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
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
    private MockMvc mockMvc; // Головний інструмент для імітації запитів

    @MockBean
    private BookService bookService; // Робимо заглушку для сервісу

    private BookDTO validBookDTO;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private com.epam.rd.autocode.spring.project.security.JwtFilter jwtFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setUp() {
        validBookDTO = new BookDTO();
        validBookDTO.setName("Test Book");
        validBookDTO.setAuthor("Test Author");
        validBookDTO.setPrice(BigDecimal.valueOf(150.00));
        validBookDTO.setPages(300);
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getAllBooks_ShouldReturnListView() throws Exception {
        // 1. Створюємо імітацію сторінки (Page) замість списку (List)
        List<BookDTO> booksList = List.of(new BookDTO());
        Page<BookDTO> bookPage = new PageImpl<>(booksList);

        // 2. Налаштовуємо mock-сервіс повертати СТОРІНКУ
        when(bookService.getFilteredAndSortedBooks(any(), any(), any(), any()))
                .thenReturn(bookPage);

        // Тут важливо, щоб тип повернення в інтерфейсі був List<String>
        when(bookService.getAllUniqueGenres())
                .thenReturn(List.of("Утопія", "Фантастика"));

        mockMvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/list"))
                // 3. Перевіряємо атрибути, які додає контролер для пагінації
                .andExpect(model().attributeExists("bookPage", "currentPage", "totalPages", "allGenres"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void showCreateForm_ShouldReturnCreateView() throws Exception {
        mockMvc.perform(get("/books/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/create"))
                .andExpect(model().attributeExists("book")); // Перевіряємо, що порожній об'єкт DTO передано у форму
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void addBook_ShouldRedirectToBooksList_WhenDataIsValid() throws Exception {
        when(bookService.addBook(any(BookDTO.class))).thenReturn(validBookDTO);

        // Імітуємо відправку форми POST-запитом
        mockMvc.perform(post("/books")
                        .with(csrf())
                        .param("name", "Test Book")
                        .param("author", "Test Author")
                        .param("price", "150.00"))
                .andExpect(status().is3xxRedirection()) // У разі успіху має бути редірект (статус 302)
                .andExpect(redirectedUrl("/books"));

        verify(bookService, times(1)).addBook(any(BookDTO.class));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void addBook_ShouldReturnCreateView_WhenValidationFails() throws Exception {
        // Відправляємо форму без обов'язкового поля "name", щоб викликати помилку валідації
        mockMvc.perform(post("/books")
                        .with(csrf())
                        .param("author", "Test Author")
                        .param("price", "150.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/create")) // Має повернути користувача назад на форму
                .andExpect(model().hasErrors()) // У моделі мають бути помилки валідації
                .andExpect(model().attributeHasFieldErrors("book", "name"));

        verify(bookService, never()).addBook(any(BookDTO.class)); // Сервіс збереження не повинен викликатися!
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void deleteBook_ShouldRedirectToBooksList() throws Exception {
        doNothing().when(bookService).deleteBookByName("Test Book");

        mockMvc.perform(post("/books/Test Book/delete")
                        .with(csrf())) // <--- ТА ДОДАЄМО СЮДИ
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books"));
    }
}