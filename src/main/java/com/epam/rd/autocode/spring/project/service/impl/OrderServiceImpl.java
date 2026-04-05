package com.epam.rd.autocode.spring.project.service.impl;

import com.epam.rd.autocode.spring.project.dto.BookItemDTO;
import com.epam.rd.autocode.spring.project.dto.OrderDTO;
import com.epam.rd.autocode.spring.project.model.Book;
import com.epam.rd.autocode.spring.project.model.BookItem;
import com.epam.rd.autocode.spring.project.model.Client;
import com.epam.rd.autocode.spring.project.model.Order;
import com.epam.rd.autocode.spring.project.repo.BookRepository;
import com.epam.rd.autocode.spring.project.repo.ClientRepository;
import com.epam.rd.autocode.spring.project.repo.EmployeeRepository;
import com.epam.rd.autocode.spring.project.repo.OrderRepository;
import com.epam.rd.autocode.spring.project.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final BookRepository bookRepository;

    @Override
    public List<OrderDTO> getOrdersByClient(String clientEmail) {
        return orderRepository.findByClient_Email(clientEmail).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderDTO> getOrdersByEmployee(String employeeEmail) {
        return orderRepository.findByEmployee_Email(employeeEmail).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderDTO addOrder(OrderDTO orderDTO) {
        // Знаходимо клієнта, який робить замовлення
        Client client = clientRepository.findByEmail(orderDTO.getClientEmail())
                .orElseThrow(() -> new RuntimeException("Клієнта не знайдено"));

        Order order = new Order();
        order.setClient(client);
        order.setOrderDate(LocalDateTime.now());
        order.setPrice(orderDTO.getPrice());

        // Якщо замовлення вже підтвердив працівник
        if (orderDTO.getEmployeeEmail() != null && !orderDTO.getEmployeeEmail().isEmpty()) {
            employeeRepository.findByEmail(orderDTO.getEmployeeEmail())
                    .ifPresent(order::setEmployee);
        }

        // Обробляємо кошик (книги)
        List<BookItem> items = orderDTO.getBookItems().stream().map(itemDTO -> {
            Book book = bookRepository.findByName(itemDTO.getBookName())
                    .orElseThrow(() -> new RuntimeException("Книгу '" + itemDTO.getBookName() + "' не знайдено"));
            return BookItem.builder()
                    .book(book)
                    .quantity(itemDTO.getQuantity())
                    .order(order)
                    .build();
        }).collect(Collectors.toList());

        order.setBookItems(items);

        Order savedOrder = orderRepository.save(order);
        return mapToDTO(savedOrder);
    }

    // Ручний мапер для Order, щоб уникнути конфліктів у ModelMapper через вкладені списки
    private OrderDTO mapToDTO(Order order) {
        return OrderDTO.builder()
                .clientEmail(order.getClient().getEmail())
                .employeeEmail(order.getEmployee() != null ? order.getEmployee().getEmail() : null)
                .orderDate(order.getOrderDate())
                .price(order.getPrice())
                .bookItems(order.getBookItems().stream()
                        .map(item -> new BookItemDTO(item.getBook().getName(), item.getQuantity()))
                        .collect(Collectors.toList()))
                .build();
    }
}