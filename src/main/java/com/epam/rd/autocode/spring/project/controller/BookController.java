package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.BookDTO;
import com.epam.rd.autocode.spring.project.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;


@Slf4j
@Controller
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;


    @GetMapping
    public String listBooks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String language,
            @RequestParam(required = false, defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Sort sort;
        if ("price_asc".equals(sortBy)) {
            sort = Sort.by(Sort.Direction.ASC, "price");
        } else if ("price_desc".equals(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "price");
        } else {
            sort = Sort.by(Sort.Direction.ASC, sortBy);
        }

        PageRequest pageable = PageRequest.of(page, size, sort);

        Page<BookDTO> bookPage = bookService.getFilteredAndSortedBooks(keyword, genre, language, pageable);

        model.addAttribute("bookPage", bookPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", bookPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("genre", genre);
        model.addAttribute("language", language);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("allGenres", bookService.getAllUniqueGenres());
        model.addAttribute("books", bookPage.getContent());

        return "books/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("book", new BookDTO());
        model.addAttribute("isEdit", false);
        return "books/create";
    }

    @PostMapping
    public String addBook(@Valid @ModelAttribute("book") BookDTO bookDTO,
                          BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            log.warn(" Помилка валідації при додаванні книги: {}", bindingResult.getAllErrors());
            model.addAttribute("isEdit", false);
            return "books/create";
        }

        bookService.addBook(bookDTO);
        log.info(" Успішно додано нову книгу: {}", bookDTO.getName());
        return "redirect:/books";
    }

    @GetMapping("/{name}/edit")
    public String showEditForm(@PathVariable String name, Model model) {
        BookDTO book = bookService.getBookByName(name);
        model.addAttribute("book", book);
        model.addAttribute("isEdit", true);
        return "books/create";
    }

    @PostMapping("/{name}/update")
    public String updateBook(@PathVariable String name,
                             @Valid @ModelAttribute("book") BookDTO bookDTO,
                             BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            log.warn(" Помилка валідації при оновленні книги '{}': {}", name, bindingResult.getAllErrors());
            model.addAttribute("isEdit", true);
            return "books/create";
        }

        bookService.updateBookByName(name, bookDTO);
        log.info(" Успішно оновлено книгу: {}", name);
        return "redirect:/books";
    }

    @PostMapping("/{name}/delete")
    public String deleteBook(@PathVariable String name) {
        bookService.deleteBookByName(name);
        log.info(" Успішно видалено книгу: {}", name);
        return "redirect:/books";
    }
}