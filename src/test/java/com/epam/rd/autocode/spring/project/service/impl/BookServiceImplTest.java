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

    @Mock
    private BookRepository bookRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private BookServiceImpl bookService;

    private Book book;
    private BookDTO bookDTO;

    @BeforeEach
    void setUp() {
        book = new Book();
        book.setId(1L);
        book.setName("Test Book");
        book.setPrice(BigDecimal.valueOf(150.0));
        book.setStockQuantity(10);

        bookDTO = new BookDTO();
        bookDTO.setName("Test Book");
        bookDTO.setPrice(BigDecimal.valueOf(150.0));
        bookDTO.setStockQuantity(10);
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
    void updateBookByName_ShouldUpdateAndReturnBook_WhenExists() {
        when(bookRepository.findByName("Test Book")).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenReturn(book);
        when(modelMapper.map(book, BookDTO.class)).thenReturn(bookDTO);

        bookDTO.setAuthor("New Author");

        BookDTO result = bookService.updateBookByName("Test Book", bookDTO);

        assertNotNull(result);
        assertEquals("Test Book", result.getName());
        verify(bookRepository, times(1)).save(book);
    }

    @Test
    void updateBookByName_ShouldThrowNotFoundException_WhenDoesNotExist() {
        when(bookRepository.findByName("Unknown")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> bookService.updateBookByName("Unknown", bookDTO));
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void updateBook_ShouldCallUpdateBookByName() {
        when(bookRepository.findByName("Test Book")).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenReturn(book);
        when(modelMapper.map(book, BookDTO.class)).thenReturn(bookDTO);

        bookService.updateBook("Test Book", bookDTO);

        verify(bookRepository, times(1)).save(book);
    }


    @Test
    void deleteBookByName_ShouldCallRepositoryDelete_WhenExists() {
        when(bookRepository.existsByName("Test Book")).thenReturn(true);
        doNothing().when(bookRepository).deleteByName("Test Book");

        bookService.deleteBookByName("Test Book");

        verify(bookRepository, times(1)).deleteByName("Test Book");
    }

    @Test
    void deleteBookByName_ShouldThrowNotFoundException_WhenDoesNotExist() {
        when(bookRepository.existsByName("Unknown")).thenReturn(false);

        assertThrows(NotFoundException.class, () -> bookService.deleteBookByName("Unknown"));
        verify(bookRepository, never()).deleteByName(anyString());
    }


    @Test
    void getAllBooks_WithKeyword_ShouldReturnFilteredPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Book> bookPage = new PageImpl<>(List.of(book));

        when(bookRepository.findByNameContainingIgnoreCase("Test", pageable)).thenReturn(bookPage);
        when(modelMapper.map(book, BookDTO.class)).thenReturn(bookDTO);

        Page<BookDTO> result = bookService.getAllBooks(pageable, "Test");

        assertEquals(1, result.getTotalElements());
        verify(bookRepository, times(1)).findByNameContainingIgnoreCase("Test", pageable);
    }

    @Test
    void getAllBooks_WithoutKeyword_ShouldReturnAllBooksPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Book> bookPage = new PageImpl<>(List.of(book));

        when(bookRepository.findAll(pageable)).thenReturn(bookPage);
        when(modelMapper.map(book, BookDTO.class)).thenReturn(bookDTO);

        Page<BookDTO> result = bookService.getAllBooks(pageable, null);

        assertEquals(1, result.getTotalElements());
        verify(bookRepository, times(1)).findAll(pageable);
    }

    @Test
    void getAllBooks_WithEmptyKeyword_ShouldReturnAllBooksPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Book> bookPage = new PageImpl<>(List.of(book));

        when(bookRepository.findAll(pageable)).thenReturn(bookPage);
        when(modelMapper.map(book, BookDTO.class)).thenReturn(bookDTO);

        Page<BookDTO> result = bookService.getAllBooks(pageable, "");

        assertEquals(1, result.getTotalElements());
        verify(bookRepository, times(1)).findAll(pageable);
    }


    @Test
    void getFilteredAndSortedBooks_WithGenre_ShouldFilterByGenre() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Book> bookPage = new PageImpl<>(List.of(book));

        when(bookRepository.findByGenre("Fantasy", pageable)).thenReturn(bookPage);
        when(modelMapper.map(book, BookDTO.class)).thenReturn(bookDTO);

        Page<BookDTO> result = bookService.getFilteredAndSortedBooks(null, "Fantasy", null, pageable);

        assertEquals(1, result.getTotalElements());
        verify(bookRepository, times(1)).findByGenre("Fantasy", pageable);
    }

    @Test
    void getFilteredAndSortedBooks_WithKeywordOnly_ShouldFilterByKeyword() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Book> bookPage = new PageImpl<>(List.of(book));

        when(bookRepository.findByNameContainingIgnoreCase("Test", pageable)).thenReturn(bookPage);
        when(modelMapper.map(book, BookDTO.class)).thenReturn(bookDTO);

        Page<BookDTO> result = bookService.getFilteredAndSortedBooks("Test", "", null, pageable);

        assertEquals(1, result.getTotalElements());
        verify(bookRepository, times(1)).findByNameContainingIgnoreCase("Test", pageable);
    }

    @Test
    void getFilteredAndSortedBooks_WithoutFilters_ShouldReturnAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Book> bookPage = new PageImpl<>(List.of(book));

        when(bookRepository.findAll(pageable)).thenReturn(bookPage);
        when(modelMapper.map(book, BookDTO.class)).thenReturn(bookDTO);

        Page<BookDTO> result = bookService.getFilteredAndSortedBooks(null, null, null, pageable);

        assertEquals(1, result.getTotalElements());
        verify(bookRepository, times(1)).findAll(pageable);
    }


    @Test
    void getAllUniqueGenres_ShouldReturnSortedAndDistinctGenres() {
        Book b1 = new Book(); b1.setGenre("Sci-Fi");
        Book b2 = new Book(); b2.setGenre("Fantasy");
        Book b3 = new Book(); b3.setGenre("Sci-Fi");
        Book b4 = new Book(); b4.setGenre("  ");
        Book b5 = new Book(); b5.setGenre(null);

        when(bookRepository.findAll()).thenReturn(List.of(b1, b2, b3, b4, b5));

        List<String> result = bookService.getAllUniqueGenres();

        assertEquals(2, result.size());
        assertEquals("Fantasy", result.get(0));
        assertEquals("Sci-Fi", result.get(1));
    }


    @Test
    void updateStock_ShouldIncreaseStock_WhenBookExists() {
        when(bookRepository.findByName("Test Book")).thenReturn(Optional.of(book));

        bookService.updateStock("Test Book", 5);

        assertEquals(15, book.getStockQuantity());
        verify(bookRepository, times(1)).save(book);
    }

    @Test
    void updateStock_ShouldHandleNullStock_WhenBookExists() {
        book.setStockQuantity(null);
        when(bookRepository.findByName("Test Book")).thenReturn(Optional.of(book));

        bookService.updateStock("Test Book", 5);

        assertEquals(5, book.getStockQuantity());
        verify(bookRepository, times(1)).save(book);
    }

    @Test
    void updateStock_ShouldThrowNotFoundException_WhenBookDoesNotExist() {
        when(bookRepository.findByName("Unknown")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> bookService.updateStock("Unknown", 5));
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void updateStock_ShouldThrowOutOfStockException_WhenQuantityBecomesNegative() {
        when(bookRepository.findByName("Test Book")).thenReturn(Optional.of(book));

        assertThrows(OutOfStockException.class, () -> bookService.updateStock("Test Book", -15));
        verify(bookRepository, never()).save(any(Book.class));
    }
}