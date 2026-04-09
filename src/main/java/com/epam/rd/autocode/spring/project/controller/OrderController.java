package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.BookItemDTO;
import com.epam.rd.autocode.spring.project.dto.OrderDTO;
import com.epam.rd.autocode.spring.project.service.OrderService;
import com.epam.rd.autocode.spring.project.service.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final BookService bookService;

    @GetMapping("/client/{email}")
    @PreAuthorize("hasAnyRole('CLIENT', 'EMPLOYEE')")
    public String getOrdersByClient(@PathVariable String email,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    @RequestParam(defaultValue = "orderDate") String sortBy,
                                    Model model,
                                    Principal principal) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));
        Page<OrderDTO> orderPage = orderService.getOrdersByClient(email, pageable);

        model.addAttribute("orderPage", orderPage);
        model.addAttribute("orders", orderPage.getContent());

        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orderPage.getTotalPages());
        model.addAttribute("role", "Client");

        return "orders/list";
    }

    @GetMapping("/employee/{email}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public String getOrdersForEmployee(@PathVariable String email,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       @RequestParam(defaultValue = "orderDate") String sortBy,
                                       Model model) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));
        Page<OrderDTO> orderPage = orderService.getEmployeeDashboardOrders(email, pageable);


        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("orderPage", orderPage);

        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orderPage.getTotalPages());
        model.addAttribute("role", "Employee");

        return "orders/list";
    }

    @PostMapping("/buy")
    public String createOrder(@RequestParam String bookName,
                              @AuthenticationPrincipal UserDetails userDetails) {

        var book = bookService.getBookByName(bookName);
        String clientEmail = userDetails.getUsername();

        OrderDTO orderDTO = OrderDTO.builder()
                .clientEmail(clientEmail)
                .price(book.getPrice())
                .bookItems(List.of(new BookItemDTO(bookName, 1)))
                .build();

        orderService.addOrder(orderDTO);
        log.info(" Клієнт {} здійснив швидке замовлення книги '{}'", clientEmail, bookName);

        return "redirect:/orders/client/" + clientEmail;
    }

    @PostMapping("/checkout")
    public String checkout(Principal principal, RedirectAttributes redirectAttributes) {
        String email = principal.getName();
        try {
            log.debug(" Початок оформлення замовлення з кошика для {}", email);
            orderService.processCheckout(email);

            log.info(" Замовлення з кошика успішно оформлено для {}", email);
            return "redirect:/profile?orderSuccess=true";

        } catch (RuntimeException e) {
            log.warn(" Помилка оформлення замовлення для {}: {}", email, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/cart";
        }
    }

    @PostMapping("/{id}/ship")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public String shipOrder(@PathVariable Long id, Principal principal) {
        String employeeEmail = principal.getName();

        orderService.shipOrder(id, employeeEmail);
        log.info("📦 Працівник {} взяв у роботу та відправив замовлення #{}", employeeEmail, id);

        return "redirect:/orders/employee/" + employeeEmail;
    }
}