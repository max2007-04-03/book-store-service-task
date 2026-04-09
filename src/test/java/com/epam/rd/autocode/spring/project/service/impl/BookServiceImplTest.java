package com.epam.rd.autocode.spring.project.service.impl;

import com.epam.rd.autocode.spring.project.dto.BookDTO;
import com.epam.rd.autocode.spring.project.exception.NotFoundException;
import com.epam.rd.autocode.spring.project.model.Book;
import com.epam.rd.autocode.spring.project.repo.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock
    private BookRepository bookRepository; // Робимо заглушку для бази даних

    @Mock
    private ModelMapper modelMapper; // Робимо заглушку для мапера

    @InjectMocks
    private BookServiceImpl bookService; // Сервіс, який ми реально тестуємо

    private Book book;
    private BookDTO bookDTO;

    @BeforeEach
    void setUp() {
        book = new Book();
        book.setId(1L);
        book.setName("Test Book");
        book.setPrice(BigDecimal.valueOf(150.0));

        bookDTO = new BookDTO();
        bookDTO.setName("Test Book");
        bookDTO.setPrice(BigDecimal.valueOf(150.0));
    }

    @Test
    void getAllBooks_ShouldReturnPageOfBooks() {
        // 1. Налаштовуємо вхідні дані для пагінації
        Pageable pageable = PageRequest.of(0, 10);
        String keyword = "Test";
        Page<Book> bookPage = new PageImpl<>(List.of(book));

        // 2. Налаштовуємо мок-репозиторій на виклик методу з параметрами
        // Оскільки в коді сервісу ви викликаєте findByNameContainingIgnoreCase, коли keyword не порожній:
        when(bookRepository.findByNameContainingIgnoreCase(keyword, pageable)).thenReturn(bookPage);
        when(modelMapper.map(book, BookDTO.class)).thenReturn(bookDTO);

        // 3. Викликаємо метод сервісу з ПРАВИЛЬНИМИ аргументами
        Page<BookDTO> result = bookService.getAllBooks(pageable, keyword);

        // 4. Перевіряємо результат
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Test Book", result.getContent().get(0).getName());

        // Перевіряємо, чи викликався саме той метод репозиторію, який ми очікували
        verify(bookRepository, times(1)).findByNameContainingIgnoreCase(keyword, pageable);
    }

    @Test
    void getBookByName_ShouldReturnBook_WhenBookExists() {
        when(bookRepository.findByName("Test Book")).thenReturn(Optional.of(book));
        when(modelMapper.map(book, BookDTO.class)).thenReturn(bookDTO);

        BookDTO result = bookService.getBookByName("Test Book");

        assertNotNull(result);
        assertEquals("Test Book", result.getName());
    }

    @Test
    void getBookByName_ShouldThrowNotFoundException_WhenBookDoesNotExist() {
        when(bookRepository.findByName("Unknown")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> bookService.getBookByName("Unknown"));
        verify(bookRepository, times(1)).findByName("Unknown");
    }

    @Test
    void addBook_ShouldSaveAndReturnBook() {
        when(modelMapper.map(bookDTO, Book.class)).thenReturn(book);
        when(bookRepository.save(book)).thenReturn(book);
        when(modelMapper.map(book, BookDTO.class)).thenReturn(bookDTO);

        BookDTO result = bookService.addBook(bookDTO);

        assertNotNull(result);
        assertEquals("Test Book", result.getName());
        verify(bookRepository, times(1)).save(book);
    }

    @Test
    void deleteBookByName_ShouldCallRepositoryDelete() {
        // 1. Додаємо цю строчку, щоб пройти перевірку в сервісі
        when(bookRepository.existsByName("Test Book")).thenReturn(true);

        // 2. Імітуємо успішне видалення
        doNothing().when(bookRepository).deleteByName("Test Book");

        // 3. Викликаємо метод
        bookService.deleteBookByName("Test Book");

        // 4. Перевіряємо, чи викликався метод видалення в репозиторії
        verify(bookRepository, times(1)).deleteByName("Test Book");
        verify(bookRepository, times(1)).existsByName("Test Book");
    }
}