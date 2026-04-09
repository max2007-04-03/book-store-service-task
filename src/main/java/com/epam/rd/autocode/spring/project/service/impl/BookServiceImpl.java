package com.epam.rd.autocode.spring.project.service.impl;

import com.epam.rd.autocode.spring.project.dto.BookDTO;
import com.epam.rd.autocode.spring.project.exception.NotFoundException;
import com.epam.rd.autocode.spring.project.exception.OutOfStockException;
import com.epam.rd.autocode.spring.project.model.Book;
import com.epam.rd.autocode.spring.project.repo.BookRepository;
import com.epam.rd.autocode.spring.project.service.BookService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final ModelMapper modelMapper;


    @Override
    public BookDTO getBookByName(String name) {
        Book book = bookRepository.findByName(name)
                .orElseThrow(() -> new NotFoundException("Книгу з назвою '" + name + "' не знайдено"));
        return modelMapper.map(book, BookDTO.class);
    }

    @Override
    @Transactional
    public BookDTO addBook(BookDTO bookDTO) {
        Book book = modelMapper.map(bookDTO, Book.class);
        Book savedBook = bookRepository.save(book);
        return modelMapper.map(savedBook, BookDTO.class);
    }

    @Override
    @Transactional
    public BookDTO updateBookByName(String name, BookDTO bookDTO) {
        Book existingBook = bookRepository.findByName(name)
                .orElseThrow(() -> new NotFoundException("Книгу з назвою '" + name + "' не знайдено"));

        existingBook.setAuthor(bookDTO.getAuthor());
        existingBook.setPrice(bookDTO.getPrice());
        existingBook.setGenre(bookDTO.getGenre());
        existingBook.setPages(bookDTO.getPages());
        existingBook.setPublicationDate(bookDTO.getPublicationDate());
        existingBook.setLanguage(bookDTO.getLanguage());
        existingBook.setAgeGroup(bookDTO.getAgeGroup());
        existingBook.setCharacteristics(bookDTO.getCharacteristics());
        existingBook.setDescription(bookDTO.getDescription());
        existingBook.setStockQuantity(bookDTO.getStockQuantity());

        Book updatedBook = bookRepository.save(existingBook);
        return modelMapper.map(updatedBook, BookDTO.class);
    }

    @Override
    @Transactional
    public void updateBook(String name, BookDTO bookDTO) {
        updateBookByName(name, bookDTO);
    }

    @Override
    @Transactional
    public void deleteBookByName(String name) {
        if (!bookRepository.existsByName(name)) {
            throw new NotFoundException("Книгу для видалення не знайдено");
        }
        bookRepository.deleteByName(name);
    }

    @Override
    public Page<BookDTO> getAllBooks(Pageable pageable, String keyword) {
        Page<Book> books;
        if (keyword != null && !keyword.isEmpty()) {
            books = bookRepository.findByNameContainingIgnoreCase(keyword, pageable);
        } else {
            books = bookRepository.findAll(pageable);
        }
        return books.map(book -> modelMapper.map(book, BookDTO.class));
    }

    @Override
    public Page<BookDTO> getFilteredAndSortedBooks(String keyword, String genre, String language, Pageable pageable) {
        Page<Book> books;

        if (genre != null && !genre.isEmpty()) {
            books = bookRepository.findByGenre(genre, pageable);
        } else if (keyword != null && !keyword.isEmpty()) {
            books = bookRepository.findByNameContainingIgnoreCase(keyword, pageable);
        } else {
            books = bookRepository.findAll(pageable);
        }

        return books.map(this::convertToDTO);
    }

    private BookDTO convertToDTO(Book book) {
        return modelMapper.map(book, BookDTO.class);
    }

    @Override
    public List<String> getAllUniqueGenres() {
        return bookRepository.findAll().stream()
                .map(Book::getGenre)
                .filter(g -> g != null && !g.trim().isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateStock(String bookName, int amount) {
        Book book = bookRepository.findByName(bookName)
                .orElseThrow(() -> new NotFoundException("Книгу '" + bookName + "' не знайдено"));

        int newQuantity = (book.getStockQuantity() != null ? book.getStockQuantity() : 0) + amount;

        if (newQuantity < 0) {
            throw new OutOfStockException("Вибачте, на складі недостатньо примірників книги '" + bookName + "'");
        }

        book.setStockQuantity(newQuantity);
        bookRepository.save(book);
    }
}