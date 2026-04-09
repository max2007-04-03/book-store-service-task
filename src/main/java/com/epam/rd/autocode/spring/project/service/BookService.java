package com.epam.rd.autocode.spring.project.service;

import com.epam.rd.autocode.spring.project.dto.BookDTO;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface BookService {


    Page<BookDTO> getAllBooks(Pageable pageable, String keyword);

    BookDTO getBookByName(String name);

    BookDTO updateBookByName(String name, BookDTO book);

    void deleteBookByName(String name);

    BookDTO addBook(BookDTO book);

    void updateBook(String name, @Valid BookDTO bookDTO);

    Object getAllUniqueGenres();

    void updateStock(String bookName, int amount);

    Page<BookDTO> getFilteredAndSortedBooks(String keyword, String genre, String language, Pageable pageable);}
