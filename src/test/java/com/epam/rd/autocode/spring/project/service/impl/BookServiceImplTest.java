package com.epam.rd.autocode.spring.project.service.impl;

import com.epam.rd.autocode.spring.project.dto.BookDTO;
import com.epam.rd.autocode.spring.project.exception.NotFoundException;
import com.epam.rd.autocode.spring.project.exception.OutOfStockException;
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

    @Mock private BookRepository bookRepository;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private BookServiceImpl bookService;

    private Book book;
    private BookDTO bookDTO;
    private final Pageable pageable = PageRequest.of(0, 10);

    @BeforeEach
    void setUp() {
        book = new Book();
        book.setName("Test Book");
        book.setPrice(BigDecimal.valueOf(150.0));
        book.setStockQuantity(10);

        bookDTO = new BookDTO();
        bookDTO.setName("Test Book");
        bookDTO.setPrice(BigDecimal.valueOf(150.0));
        bookDTO.setStockQuantity(10);
    }

    @Test
    void getBookByName_LogicTest() {
        when(bookRepository.findByName("Test Book")).thenReturn(Optional.of(book));
        when(modelMapper.map(book, BookDTO.class)).thenReturn(bookDTO);
        assertNotNull(bookService.getBookByName("Test Book"));

        when(bookRepository.findByName("Unknown")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> bookService.getBookByName("Unknown"));
    }

    @Test
    void addBook_ShouldSaveSuccessfully() {
        when(modelMapper.map(any(BookDTO.class), eq(Book.class))).thenReturn(book);
        when(bookRepository.save(book)).thenReturn(book);
        when(modelMapper.map(book, BookDTO.class)).thenReturn(bookDTO);

        assertNotNull(bookService.addBook(bookDTO));
        verify(bookRepository).save(book);
    }

    @Test
    void updateAndBookManagement_LogicTest() {
        when(bookRepository.findByName("Test Book")).thenReturn(Optional.of(book));
        doNothing().when(modelMapper).map(any(BookDTO.class), any(Book.class));
        when(bookRepository.save(any(Book.class))).thenReturn(book);
        when(modelMapper.map(any(Book.class), eq(BookDTO.class))).thenReturn(bookDTO);

        bookService.updateBook("Test Book", bookDTO);
        assertNotNull(bookService.updateBookByName("Test Book", bookDTO));

        when(bookRepository.existsByName("Test Book")).thenReturn(true);
        bookService.deleteBookByName("Test Book");

        when(bookRepository.existsByName("Unknown")).thenReturn(false);
        assertThrows(NotFoundException.class, () -> bookService.deleteBookByName("Unknown"));
    }

    @Test
    void getFilteredBooks_BranchCoverageTest() {
        Page<Book> bookPage = new PageImpl<>(List.of(book));
        when(modelMapper.map(any(Book.class), eq(BookDTO.class))).thenReturn(bookDTO);

        when(bookRepository.findByGenre("Fantasy", pageable)).thenReturn(bookPage);
        bookService.getFilteredAndSortedBooks(null, "Fantasy", null, pageable);

        when(bookRepository.findByNameContainingIgnoreCase("Key", pageable)).thenReturn(bookPage);
        bookService.getFilteredAndSortedBooks("Key", "", null, pageable);

        when(bookRepository.findAll(pageable)).thenReturn(bookPage);
        bookService.getFilteredAndSortedBooks(null, null, null, pageable);

        bookService.getAllBooks(pageable, "Key");
        bookService.getAllBooks(pageable, "");

        verify(bookRepository, times(2)).findAll(pageable);
    }

    @Test
    void getAllUniqueGenres_LogicTest() {
        Book b1 = new Book(); b1.setGenre("Fantasy");
        Book b2 = new Book(); b2.setGenre("   ");
        Book b3 = new Book(); b3.setGenre(null);
        when(bookRepository.findAll()).thenReturn(List.of(b1, b2, b3));

        List<String> result = bookService.getAllUniqueGenres();

        assertEquals(1, result.size());
        assertEquals("Fantasy", result.get(0));
    }

    @Test
    void updateStock_FullLogicTest() {
        when(bookRepository.findByName("Test Book")).thenReturn(Optional.of(book));

        book.setStockQuantity(null);
        bookService.updateStock("Test Book", 5);
        assertEquals(5, book.getStockQuantity());

        assertThrows(OutOfStockException.class, () -> bookService.updateStock("Test Book", -10));

        when(bookRepository.findByName("None")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> bookService.updateStock("None", 1));
    }

    @Test
    void updateStock_TernaryAndExceptions() {
        when(bookRepository.findByName("Test Book")).thenReturn(Optional.of(book));

        book.setStockQuantity(null);
        bookService.updateStock("Test Book", 5);
        assertEquals(5, book.getStockQuantity());

        assertThrows(OutOfStockException.class, () -> bookService.updateStock("Test Book", -10));
    }




}