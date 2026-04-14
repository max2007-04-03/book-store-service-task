package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.BookDTO;
import com.epam.rd.autocode.spring.project.service.BookService;
import com.epam.rd.autocode.spring.project.service.impl.CartService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Slf4j
@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final BookService bookService;
    private final CartService cartService;

    @GetMapping
    public String viewCart(Model model, Principal principal) {
        if (principal != null) {
            model.addAttribute("cartItems", cartService.getCartItems(principal.getName()));
            model.addAttribute("cartTotal", cartService.getCartTotal(principal.getName()));
        }
        return "cart/view";
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam String bookName,
                            @RequestParam(defaultValue = "1") int quantity,
                            Principal principal,
                            @RequestHeader(value = "Referer", required = false) String referer,
                            RedirectAttributes redirectAttributes) {

        BookDTO book = bookService.getBookByName(bookName);

        if (book.getStockQuantity() == null || book.getStockQuantity() < quantity) {
            log.warn(" Спроба додати в кошик книгу '{}', якої немає в достатній кількості", bookName);
            redirectAttributes.addFlashAttribute("errorMessage", "Вибачте, книги '" + bookName + "' недостатньо на складі.");
            return redirectToPreviousPage(referer);
        }

        cartService.addItemToDatabaseCart(principal.getName(), bookName, quantity, book.getPrice());
        log.info(" Користувач {} додав у кошик '{}' ({} шт.)", principal.getName(), bookName, quantity);

        return redirectToPreviousPageWithParam(referer, "added_to_cart");
    }

    @PostMapping("/remove")
    public String removeFromCart(@RequestParam String bookName, Principal principal) {
        cartService.removeItemFromDatabaseCart(principal.getName(), bookName);
        log.info(" Користувач {} видалив з кошика '{}'", principal.getName(), bookName);
        return "redirect:/cart";
    }


    private String redirectToPreviousPage(String referer) {
        return "redirect:" + (referer != null ? referer : "/books");
    }

    private String redirectToPreviousPageWithParam(String referer, String param) {
        if (referer == null) return "redirect:/books?" + param;
        if (referer.contains(param)) return "redirect:" + referer;
        return "redirect:" + referer + (referer.contains("?") ? "&" : "?") + param;
    }
}