package com.epam.rd.autocode.spring.project.service.impl;

import com.epam.rd.autocode.spring.project.dto.BookItemDTO;
import com.epam.rd.autocode.spring.project.dto.OrderDTO;
import com.epam.rd.autocode.spring.project.exception.NotFoundException;
import com.epam.rd.autocode.spring.project.model.*;
import com.epam.rd.autocode.spring.project.model.enums.OrderStatus;
import com.epam.rd.autocode.spring.project.repo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
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
    @Mock private CartRepository cartRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order order;
    private OrderDTO orderDTO;
    private Client client;
    private Book book;
    private Employee employee;
    private CartItem cartItem;
    private final String CLIENT_EMAIL = "client@test.com";

    @BeforeEach
    void setUp() {
        client = new Client();
        client.setEmail(CLIENT_EMAIL);
        client.setBalance(BigDecimal.valueOf(1000.0));

        book = new Book();
        book.setName("Test Book");
        book.setStockQuantity(10);

        employee = new Employee();
        employee.setEmail("emp@test.com");

        BookItem bookItem = new BookItem();
        bookItem.setBook(book);
        bookItem.setQuantity(2);

        order = new Order();
        order.setId(1L);
        order.setClient(client);
        order.setOrderDate(LocalDateTime.now());
        order.setPrice(BigDecimal.valueOf(300.0));
        order.setStatus(OrderStatus.PAID);
        order.setBookItems(List.of(bookItem));
        bookItem.setOrder(order);

        BookItemDTO bookItemDTO = new BookItemDTO("Test Book", 2);
        orderDTO = new OrderDTO();
        orderDTO.setClientEmail(CLIENT_EMAIL);
        orderDTO.setPrice(BigDecimal.valueOf(300.0));
        orderDTO.setBookItems(List.of(bookItemDTO));

        cartItem = new CartItem();
        cartItem.setClientEmail(CLIENT_EMAIL);
        cartItem.setBookName("Test Book");
        cartItem.setQuantity(2);
        cartItem.setPrice(BigDecimal.valueOf(150.0));
    }


    @Test
    void getOrdersByClient_ShouldReturnOrders() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(List.of(order));

        when(orderRepository.findByClient_Email(eq(CLIENT_EMAIL), any(Pageable.class))).thenReturn(orderPage);

        Page<OrderDTO> result = orderService.getOrdersByClient(CLIENT_EMAIL, pageable);

        assertEquals(1, result.getContent().size());
        verify(orderRepository, times(1)).findByClient_Email(eq(CLIENT_EMAIL), any(Pageable.class));
    }


    @Test
    void getOrdersByEmployee_ShouldReturnOrders() {
        order.setEmployee(employee);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(List.of(order));

        when(orderRepository.findByEmployee_Email(eq("emp@test.com"), any(Pageable.class))).thenReturn(orderPage);

        Page<OrderDTO> result = orderService.getOrdersByEmployee("emp@test.com", pageable);

        assertEquals(1, result.getContent().size());
        assertEquals("emp@test.com", result.getContent().get(0).getEmployeeEmail());
    }


    @Test
    void getAllPaidOrders_ShouldReturnOnlyPaidOrders() {
        Order unpaidOrder = new Order();
        unpaidOrder.setId(2L);
        unpaidOrder.setClient(client);
        unpaidOrder.setStatus(OrderStatus.SHIPPED);
        unpaidOrder.setBookItems(Collections.emptyList());

        when(orderRepository.findAll()).thenReturn(List.of(order, unpaidOrder));

        List<OrderDTO> result = orderService.getAllPaidOrders();

        assertEquals(1, result.size());
        assertEquals(OrderStatus.PAID, result.get(0).getStatus());
    }


    @Test
    void processCheckout_ShouldCreateOrder_WhenValid() {
        when(cartRepository.findByClientEmail(CLIENT_EMAIL)).thenReturn(List.of(cartItem));
        when(clientRepository.findByEmail(CLIENT_EMAIL)).thenReturn(Optional.of(client));
        when(bookRepository.findByName("Test Book")).thenReturn(Optional.of(book));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        orderService.processCheckout(CLIENT_EMAIL);

        assertEquals(8, book.getStockQuantity());
        assertEquals(0, BigDecimal.valueOf(700.0).compareTo(client.getBalance()));

        verify(bookRepository, times(1)).save(book);
        verify(clientRepository, times(1)).save(client);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(cartRepository, times(1)).deleteAll(anyList());
    }

    @Test
    void processCheckout_ShouldThrowNotFoundException_WhenClientNotFound() {
        when(cartRepository.findByClientEmail(CLIENT_EMAIL)).thenReturn(List.of(cartItem));
        when(clientRepository.findByEmail(CLIENT_EMAIL)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> orderService.processCheckout(CLIENT_EMAIL));
    }

    @Test
    void processCheckout_ShouldThrowRuntimeException_WhenCartIsEmpty() {
        when(cartRepository.findByClientEmail(CLIENT_EMAIL)).thenReturn(Collections.emptyList());
        when(clientRepository.findByEmail(CLIENT_EMAIL)).thenReturn(Optional.of(client));

        assertThrows(RuntimeException.class, () -> orderService.processCheckout(CLIENT_EMAIL));
    }

    @Test
    void processCheckout_ShouldThrowNotFoundException_WhenBookNotFound() {
        when(cartRepository.findByClientEmail(CLIENT_EMAIL)).thenReturn(List.of(cartItem));
        when(clientRepository.findByEmail(CLIENT_EMAIL)).thenReturn(Optional.of(client));
        when(bookRepository.findByName("Test Book")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> orderService.processCheckout(CLIENT_EMAIL));
    }

    @Test
    void processCheckout_ShouldThrowRuntimeException_WhenOutOfStock() {
        book.setStockQuantity(1); // Менше, ніж у кошику (2)
        when(cartRepository.findByClientEmail(CLIENT_EMAIL)).thenReturn(List.of(cartItem));
        when(clientRepository.findByEmail(CLIENT_EMAIL)).thenReturn(Optional.of(client));
        when(bookRepository.findByName("Test Book")).thenReturn(Optional.of(book));

        assertThrows(RuntimeException.class, () -> orderService.processCheckout(CLIENT_EMAIL));
    }

    @Test
    void processCheckout_ShouldThrowRuntimeException_WhenInsufficientFunds() {
        client.setBalance(BigDecimal.valueOf(100.0)); // Менше, ніж сума (300)
        when(cartRepository.findByClientEmail(CLIENT_EMAIL)).thenReturn(List.of(cartItem));
        when(clientRepository.findByEmail(CLIENT_EMAIL)).thenReturn(Optional.of(client));
        when(bookRepository.findByName("Test Book")).thenReturn(Optional.of(book));

        assertThrows(RuntimeException.class, () -> orderService.processCheckout(CLIENT_EMAIL));
    }


    @Test
    void shipOrder_ShouldUpdateStatusAndEmployee() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(employeeRepository.findByEmail("emp@test.com")).thenReturn(Optional.of(employee));

        orderService.shipOrder(1L, "emp@test.com");

        assertEquals(OrderStatus.SHIPPED, order.getStatus());
        assertEquals(employee, order.getEmployee());
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void shipOrder_ShouldThrowNotFoundException_WhenOrderNotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> orderService.shipOrder(1L, "emp@test.com"));
    }

    @Test
    void shipOrder_ShouldThrowNotFoundException_WhenEmployeeNotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(employeeRepository.findByEmail("emp@test.com")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> orderService.shipOrder(1L, "emp@test.com"));
    }


    @Test
    void getNewShippedOrders_ShouldReturnUnnotifiedShippedOrders() {
        order.setStatus(OrderStatus.SHIPPED);
        order.setNotified(false);

        when(orderRepository.findByClient_Email(eq(CLIENT_EMAIL), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(order)));

        List<OrderDTO> result = orderService.getNewShippedOrders(CLIENT_EMAIL);

        assertEquals(1, result.size());
        assertTrue(order.isNotified());
    }


    @Test
    void getEmployeeDashboardOrders_ShouldReturnOrders() {
        Pageable pageable = PageRequest.of(0, 10);
        when(orderRepository.findAllForEmployeeDashboard("emp@test.com", pageable))
                .thenReturn(new PageImpl<>(List.of(order)));

        Page<OrderDTO> result = orderService.getEmployeeDashboardOrders("emp@test.com", pageable);

        assertEquals(1, result.getContent().size());
    }


    @Test
    void addOrder_ShouldSaveAndReturnOrder() {
        when(clientRepository.findByEmail(CLIENT_EMAIL)).thenReturn(Optional.of(client));
        when(bookRepository.findByName("Test Book")).thenReturn(Optional.of(book));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderDTO result = orderService.addOrder(orderDTO);

        assertNotNull(result);
        assertEquals(CLIENT_EMAIL, result.getClientEmail());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void addOrder_ShouldThrowNotFoundException_WhenClientNotFound() {
        when(clientRepository.findByEmail(CLIENT_EMAIL)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> orderService.addOrder(orderDTO));
    }

    @Test
    void addOrder_ShouldThrowNotFoundException_WhenBookNotFound() {
        when(clientRepository.findByEmail(CLIENT_EMAIL)).thenReturn(Optional.of(client));
        when(bookRepository.findByName("Test Book")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> orderService.addOrder(orderDTO));
    }


    @Test
    void getAllOrders_WithSearchKeyword_ShouldReturnFilteredOrders() {
        Pageable pageable = PageRequest.of(0, 10);
        when(orderRepository.findByOrderNumberContainingIgnoreCase("123", pageable))
                .thenReturn(new PageImpl<>(List.of(order)));

        Page<OrderDTO> result = orderService.getAllOrders(" 123 ", pageable);

        assertEquals(1, result.getContent().size());
        verify(orderRepository, times(1)).findByOrderNumberContainingIgnoreCase("123", pageable);
    }

    @Test
    void getAllOrders_WithEmptySearchKeyword_ShouldReturnAllOrders() {
        Pageable pageable = PageRequest.of(0, 10);
        when(orderRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(order)));

        Page<OrderDTO> result = orderService.getAllOrders("   ", pageable);

        assertEquals(1, result.getContent().size());
        verify(orderRepository, times(1)).findAll(pageable);
    }

    @Test
    void getAllOrders_WithNullSearchKeyword_ShouldReturnAllOrders() {
        Pageable pageable = PageRequest.of(0, 10);
        when(orderRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(order)));

        Page<OrderDTO> result = orderService.getAllOrders(null, pageable);

        assertEquals(1, result.getContent().size());
        verify(orderRepository, times(1)).findAll(pageable);
    }
}