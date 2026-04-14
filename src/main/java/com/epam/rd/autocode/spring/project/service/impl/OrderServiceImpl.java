package com.epam.rd.autocode.spring.project.service.impl;

import com.epam.rd.autocode.spring.project.dto.BookItemDTO;
import com.epam.rd.autocode.spring.project.dto.OrderDTO;
import com.epam.rd.autocode.spring.project.exception.NotFoundException;
import com.epam.rd.autocode.spring.project.model.*;
import com.epam.rd.autocode.spring.project.model.enums.OrderStatus;
import com.epam.rd.autocode.spring.project.repo.*;
import com.epam.rd.autocode.spring.project.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final BookRepository bookRepository;
    private final CartRepository cartRepository;

    @Override
    public Page<OrderDTO> getOrdersByClient(String clientEmail, Pageable pageable) {
        return orderRepository.findByClient_Email(clientEmail, pageable)
                .map(this::mapToDTO);
    }

    @Override
    public Page<OrderDTO> getOrdersByEmployee(String employeeEmail, Pageable pageable) {
        return orderRepository.findByEmployee_Email(employeeEmail, pageable)
                .map(this::mapToDTO);
    }

    @Override
    public List<OrderDTO> getAllPaidOrders() {
        return orderRepository.findAll().stream()
                .filter(order -> order.getStatus() == OrderStatus.PAID)
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void processCheckout(String email) {
        List<CartItem> cartItems = cartRepository.findByClientEmail(email);
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Клієнта не знайдено"));

        if (cartItems.isEmpty()) {
            throw new RuntimeException("Кошик порожній");
        }

        BigDecimal totalPrice = BigDecimal.ZERO;
        List<BookItemDTO> bookItemDTOs = new java.util.ArrayList<>();

        for (CartItem item : cartItems) {
            Book book = bookRepository.findByName(item.getBookName())
                    .orElseThrow(() -> new NotFoundException("Книгу не знайдено"));

            if (book.getStockQuantity() < item.getQuantity()) {
                throw new RuntimeException("Недостатньо книг на складі: " + item.getBookName());
            }

            book.setStockQuantity(book.getStockQuantity() - item.getQuantity());
            bookRepository.save(book);

            totalPrice = totalPrice.add(item.getTotalPrice());
            bookItemDTOs.add(new BookItemDTO(item.getBookName(), item.getQuantity()));
        }

        if (client.getBalance().compareTo(totalPrice) < 0) {
            throw new RuntimeException("Недостатньо коштів на балансі!");
        }

        client.setBalance(client.getBalance().subtract(totalPrice));
        clientRepository.save(client);


        Order order = Order.builder()
                .client(client)
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.PAID)
                .price(totalPrice)
                .orderNumber(String.valueOf((int)(Math.random() * 900000) + 100000))
                .notified(false)
                .build();

        List<BookItem> items = bookItemDTOs.stream()
                .map(dto -> BookItem.builder()
                        .book(bookRepository.findByName(dto.getBookName()).get())
                        .quantity(dto.getQuantity())
                        .order(order)
                        .build())
                .collect(Collectors.toList());

        order.setBookItems(items);
        orderRepository.save(order);

        cartRepository.deleteAll(cartItems);

        log.info(" Замовлення #{} успішно створено для {}", order.getOrderNumber(), email);
    }

    @Override
    @Transactional
    public void shipOrder(Long id, String employeeEmail) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Замовлення не знайдено"));
        Employee employee = employeeRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new NotFoundException("Працівника не знайдено"));

        order.setEmployee(employee);
        order.setStatus(OrderStatus.SHIPPED);
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public List<OrderDTO> getNewShippedOrders(String clientEmail) {
        return orderRepository.findByClient_Email(clientEmail, Pageable.unpaged()).stream()
                .filter(order -> order.getStatus() == OrderStatus.SHIPPED && !order.isNotified())
                .peek(order -> order.setNotified(true))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<OrderDTO> getEmployeeDashboardOrders(String employeeEmail, Pageable pageable) {
        return orderRepository.findAllForEmployeeDashboard(employeeEmail, pageable)
                .map(this::mapToDTO);
    }

    @Override
    @Transactional
    public OrderDTO addOrder(OrderDTO orderDTO) {
        Client client = clientRepository.findByEmail(orderDTO.getClientEmail())
                .orElseThrow(() -> new NotFoundException("Клієнт не знайдений"));

        Order order = Order.builder()
                .client(client)
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.PAID)
                .price(orderDTO.getPrice())
                .orderNumber(String.valueOf((int)(Math.random() * 900000) + 100000))
                .build();

        List<BookItem> items = orderDTO.getBookItems().stream()
                .map(dto -> BookItem.builder()
                        .book(bookRepository.findByName(dto.getBookName())
                                .orElseThrow(() -> new NotFoundException("Книга не знайдена")))
                        .quantity(dto.getQuantity())
                        .order(order)
                        .build())
                .collect(Collectors.toList());

        order.setBookItems(items);
        return mapToDTO(orderRepository.save(order));
    }

    private OrderDTO mapToDTO(Order order) {
        return OrderDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .clientEmail(order.getClient().getEmail())
                .employeeEmail(order.getEmployee() != null ? order.getEmployee().getEmail() : null)
                .orderDate(order.getOrderDate())
                .price(order.getPrice())
                .bookItems(order.getBookItems().stream()
                        .map(item -> new BookItemDTO(item.getBook().getName(), item.getQuantity()))
                        .collect(Collectors.toList()))
                .build();
    }

    @Override
    public Page<OrderDTO> getAllOrders(String search, Pageable pageable) {
        Page<Order> orders;

        if (search != null && !search.trim().isEmpty()) {
            orders = orderRepository.findByOrderNumberContainingIgnoreCase(search.trim(), pageable);
        } else {
            orders = orderRepository.findAll(pageable);
        }

        return orders.map(this::mapToDTO);
    }
}