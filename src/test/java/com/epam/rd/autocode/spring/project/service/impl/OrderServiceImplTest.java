package com.epam.rd.autocode.spring.project.service.impl;

import com.epam.rd.autocode.spring.project.dto.BookItemDTO;
import com.epam.rd.autocode.spring.project.dto.OrderDTO;
import com.epam.rd.autocode.spring.project.exception.NotFoundException;
import com.epam.rd.autocode.spring.project.model.*;
import com.epam.rd.autocode.spring.project.repo.BookRepository;
import com.epam.rd.autocode.spring.project.repo.ClientRepository;
import com.epam.rd.autocode.spring.project.repo.EmployeeRepository;
import com.epam.rd.autocode.spring.project.repo.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private BookRepository bookRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order order;
    private OrderDTO orderDTO;
    private Client client;
    private Book book;
    private final String CLIENT_EMAIL = "client@test.com";

    @BeforeEach
    void setUp() {
        client = new Client();
        client.setEmail(CLIENT_EMAIL);

        book = new Book();
        book.setName("Test Book");

        BookItem bookItem = new BookItem();
        bookItem.setBook(book);
        bookItem.setQuantity(2);

        order = new Order();
        order.setId(1L);
        order.setClient(client);
        order.setOrderDate(LocalDateTime.now());
        order.setPrice(BigDecimal.valueOf(300.0));
        order.setBookItems(List.of(bookItem));
        bookItem.setOrder(order);

        BookItemDTO bookItemDTO = new BookItemDTO("Test Book", 2);
        orderDTO = new OrderDTO();
        orderDTO.setClientEmail(CLIENT_EMAIL);
        orderDTO.setPrice(BigDecimal.valueOf(300.0));
        orderDTO.setBookItems(List.of(bookItemDTO));
    }

    @Test
    void getOrdersByClient_ShouldReturnOrders() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(List.of(order));

        when(orderRepository.findByClient_Email(eq(CLIENT_EMAIL), any(Pageable.class))).thenReturn(orderPage);

        Page<OrderDTO> result = orderService.getOrdersByClient(CLIENT_EMAIL, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(orderRepository, times(1)).findByClient_Email(eq(CLIENT_EMAIL), any(Pageable.class));
    }

    @Test
    void getOrdersByEmployee_ShouldReturnOrders() {
        // 🏆 ТУТ БУЛИ ОСНОВНІ ВИПРАВЛЕННЯ
        String employeeEmail = "emp@test.com";
        Employee employee = new Employee();
        employee.setEmail(employeeEmail);
        order.setEmployee(employee);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(List.of(order));

        // Репозиторій тепер очікує (String, Pageable)
        when(orderRepository.findByEmployee_Email(eq(employeeEmail), any(Pageable.class))).thenReturn(orderPage);

        // Сервіс тепер повертає Page<OrderDTO>
        Page<OrderDTO> result = orderService.getOrdersByEmployee(employeeEmail, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(employeeEmail, result.getContent().get(0).getEmployeeEmail());
        verify(orderRepository, times(1)).findByEmployee_Email(eq(employeeEmail), any(Pageable.class));
    }

    @Test
    void addOrder_ShouldSaveAndReturnOrder() {
        when(clientRepository.findByEmail(CLIENT_EMAIL)).thenReturn(Optional.of(client));
        when(bookRepository.findByName("Test Book")).thenReturn(Optional.of(book));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderDTO result = orderService.addOrder(orderDTO);

        assertNotNull(result);
        assertEquals(CLIENT_EMAIL, result.getClientEmail());
        assertEquals(1, result.getBookItems().size());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    // Решта тестів на Exception залишаються без змін, вони валідні
}