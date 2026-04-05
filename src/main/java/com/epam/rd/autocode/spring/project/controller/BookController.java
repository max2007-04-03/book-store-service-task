package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.BookDTO;
import com.epam.rd.autocode.spring.project.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;


    @GetMapping
    public String getAllBooks(Model model) {
        model.addAttribute("books", bookService.getAllBooks());
        return "books/list";
    }


    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("book", new BookDTO());
        return "books/create";
    }


    @PostMapping
    public String addBook(@Valid @ModelAttribute("book") BookDTO bookDTO,
                          BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "books/create";
        }
        bookService.addBook(bookDTO);
        return "redirect:/books";
    }


    @PostMapping("/{name}/delete")
    public String deleteBook(@PathVariable String name) {
        bookService.deleteBookByName(name);
        return "redirect:/books";
    }
}