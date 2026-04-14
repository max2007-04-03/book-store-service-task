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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private BookRepository bookRepository;
    @Mock private CartRepository cartRepository;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order order;
    private OrderDTO orderDTO;
    private Client client;
    private Book book;
    private Employee employee;
    private CartItem cartItem;
    private final String clientEmail = "client@test.com";

    @BeforeEach
    void setUp() {
        client = new Client();
        client.setEmail(clientEmail);
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

        orderDTO = new OrderDTO();
        orderDTO.setClientEmail(clientEmail);
        orderDTO.setPrice(BigDecimal.valueOf(300.0));
        orderDTO.setBookItems(List.of(new BookItemDTO("Test Book", 2)));

        cartItem = new CartItem();
        cartItem.setClientEmail(clientEmail);
        cartItem.setBookName("Test Book");
        cartItem.setQuantity(2);
        cartItem.setPrice(BigDecimal.valueOf(150.0));

        when(modelMapper.map(any(), eq(OrderDTO.class))).thenAnswer(invocation -> {
            OrderDTO dto = new OrderDTO();
            Order source = invocation.getArgument(0);
            if (source != null && source.getClient() != null) {
                dto.setClientEmail(source.getClient().getEmail());
            }
            if (source != null && source.getEmployee() != null) {
                dto.setEmployeeEmail(source.getEmployee().getEmail());
            }
            dto.setStatus(source != null ? source.getStatus() : null);
            return dto;
        });
    }

    @Test
    void getOrdersByClient_ShouldReturnOrders() {
        Pageable pageable = PageRequest.of(0, 10);
        when(orderRepository.findByClient_Email(anyString(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(order)));

        Page<OrderDTO> result = orderService.getOrdersByClient(clientEmail, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getOrdersByEmployee_ShouldReturnOrders() {
        order.setEmployee(employee);
        Pageable pageable = PageRequest.of(0, 10);
        when(orderRepository.findByEmployee_Email(eq("emp@test.com"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));

        Page<OrderDTO> result = orderService.getOrdersByEmployee("emp@test.com", pageable);

        assertEquals(1, result.getContent().size());
        assertEquals("emp@test.com", result.getContent().get(0).getEmployeeEmail());
    }

    @Test
    void getEmployeeDashboardOrders_ShouldReturnOrdersPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(orderRepository.findAllForEmployeeDashboard(eq("emp@test.com"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));

        Page<OrderDTO> result = orderService.getEmployeeDashboardOrders("emp@test.com", pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getAllOrders_LogicBranchesTest() {
        Pageable pageable = PageRequest.of(0, 10);
        when(orderRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(order)));
        when(orderRepository.findByOrderNumberContainingIgnoreCase("123", pageable))
                .thenReturn(new PageImpl<>(List.of(order)));

        orderService.getAllOrders("123", pageable);
        verify(orderRepository).findByOrderNumberContainingIgnoreCase("123", pageable);

        orderService.getAllOrders("   ", pageable);
        orderService.getAllOrders(null, pageable);
        verify(orderRepository, times(2)).findAll(pageable);
    }

    @Test
    void getAllPaidOrders_ShouldReturnOnlyPaidOrders() {
        Order unpaid = new Order();
        unpaid.setStatus(OrderStatus.SHIPPED);
        unpaid.setClient(client);
        unpaid.setBookItems(Collections.emptyList());

        when(orderRepository.findAll()).thenReturn(List.of(order, unpaid));

        List<OrderDTO> result = orderService.getAllPaidOrders();
        assertEquals(1, result.size());
        assertEquals(OrderStatus.PAID, result.get(0).getStatus());
    }

    @Test
    @Transactional
    void processCheckout_LogicBranchesTest() {
        when(cartRepository.findByClientEmail(clientEmail)).thenReturn(List.of(cartItem));
        when(clientRepository.findByEmail(clientEmail)).thenReturn(Optional.of(client));
        when(bookRepository.findByName("Test Book")).thenReturn(Optional.of(book));

        orderService.processCheckout(clientEmail);
        verify(orderRepository).save(any(Order.class));
        verify(cartRepository).deleteAll(any());

        when(cartRepository.findByClientEmail(clientEmail)).thenReturn(Collections.emptyList());
        assertThrows(RuntimeException.class, () -> orderService.processCheckout(clientEmail));

        when(clientRepository.findByEmail(clientEmail)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> orderService.processCheckout(clientEmail));
    }

    @Test
    void addOrder_LogicBranchesTest() {
        when(clientRepository.findByEmail(clientEmail)).thenReturn(Optional.of(client));
        when(bookRepository.findByName(anyString())).thenReturn(Optional.of(book));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        assertNotNull(orderService.addOrder(orderDTO));

        when(bookRepository.findByName(anyString())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> orderService.addOrder(orderDTO));

        when(clientRepository.findByEmail(clientEmail)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> orderService.addOrder(orderDTO));
    }

    @Test
    void shipOrder_LogicBranchesTest() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(employeeRepository.findByEmail("emp@test.com")).thenReturn(Optional.of(employee));

        orderService.shipOrder(1L, "emp@test.com");
        assertEquals(OrderStatus.SHIPPED, order.getStatus());

        when(orderRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> orderService.shipOrder(1L, "emp@test.com"));
    }

    @Test
    void getNewShippedOrders_BranchCoverage() {
        order.setStatus(OrderStatus.SHIPPED);
        order.setNotified(false);

        Order paidOrder = new Order();
        paidOrder.setStatus(OrderStatus.PAID);
        paidOrder.setNotified(false);

        Order notifiedOrder = new Order();
        notifiedOrder.setStatus(OrderStatus.SHIPPED);
        notifiedOrder.setNotified(true);

        when(orderRepository.findByClient_Email(eq(clientEmail), any()))
                .thenReturn(new PageImpl<>(List.of(order, paidOrder, notifiedOrder)));

        List<OrderDTO> result = orderService.getNewShippedOrders(clientEmail);
        assertEquals(1, result.size());
    }

    @Test
    void getNewShippedOrders_FullCoverageTest() {
        order.setStatus(OrderStatus.SHIPPED);
        order.setNotified(false);
        order.setEmployee(employee);

        Order alreadyNotified = new Order();
        alreadyNotified.setStatus(OrderStatus.SHIPPED);
        alreadyNotified.setNotified(true);
        alreadyNotified.setClient(client);
        alreadyNotified.setBookItems(Collections.emptyList());

        Order paidOrder = new Order();
        paidOrder.setStatus(OrderStatus.PAID);
        paidOrder.setNotified(false);
        paidOrder.setClient(client);
        paidOrder.setBookItems(Collections.emptyList());

        when(orderRepository.findByClient_Email(eq(clientEmail), any()))
                .thenReturn(new PageImpl<>(List.of(order, alreadyNotified, paidOrder)));

        List<OrderDTO> result = orderService.getNewShippedOrders(clientEmail);


        assertEquals(1, result.size());
        assertTrue(order.isNotified());
        assertEquals("emp@test.com", result.get(0).getEmployeeEmail());
        verify(modelMapper, times(1)).map(any(), eq(OrderDTO.class));
    }

    @Test
    void processCheckout_ShouldThrowException_WhenStockIsInsufficient() {
        cartItem.setQuantity(2);
        book.setStockQuantity(1);

        when(cartRepository.findByClientEmail(clientEmail)).thenReturn(List.of(cartItem));
        when(clientRepository.findByEmail(clientEmail)).thenReturn(Optional.of(client));
        when(bookRepository.findByName(anyString())).thenReturn(Optional.of(book));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> orderService.processCheckout(clientEmail));
        assertTrue(ex.getMessage().contains("Недостатньо книг на складі"));
    }

    @Test
    void processCheckout_ShouldThrowException_WhenBalanceIsTooLow() {
        client.setBalance(BigDecimal.valueOf(100.0));
        when(cartRepository.findByClientEmail(clientEmail)).thenReturn(List.of(cartItem));
        when(clientRepository.findByEmail(clientEmail)).thenReturn(Optional.of(client));
        when(bookRepository.findByName(anyString())).thenReturn(Optional.of(book));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> orderService.processCheckout(clientEmail));
        assertEquals("Недостатньо коштів на балансі!", ex.getMessage());
    }

    @Test
    void processCheckout_FullBranchCoverage() {
        when(cartRepository.findByClientEmail(clientEmail)).thenReturn(List.of(cartItem));
        when(clientRepository.findByEmail(clientEmail)).thenReturn(Optional.of(client));
        when(bookRepository.findByName(anyString())).thenReturn(Optional.of(book));

        book.setStockQuantity(1);
        assertThrows(RuntimeException.class, () -> orderService.processCheckout(clientEmail));

        book.setStockQuantity(10);
        client.setBalance(BigDecimal.ZERO);
        assertThrows(RuntimeException.class, () -> orderService.processCheckout(clientEmail));
    }

    @Test
    void getAllOrders_FullBranchCoverage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(orderRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(order)));

        orderService.getAllOrders(null, pageable);

        orderService.getAllOrders("   ", pageable);

        verify(orderRepository, times(2)).findAll(pageable);
    }

    @Test
    void processCheckout_InsufficientBalanceBranch() {
        when(cartRepository.findByClientEmail(clientEmail)).thenReturn(List.of(cartItem));
        when(clientRepository.findByEmail(clientEmail)).thenReturn(Optional.of(client));
        when(bookRepository.findByName(anyString())).thenReturn(Optional.of(book));

        client.setBalance(BigDecimal.ZERO);
        assertThrows(RuntimeException.class, () -> orderService.processCheckout(clientEmail));
    }

    @Test
    void getNewShippedOrders_AllBranchCombinations() {
        order.setStatus(OrderStatus.SHIPPED);
        order.setNotified(false);

        Order alreadyNotified = new Order();
        alreadyNotified.setStatus(OrderStatus.SHIPPED);
        alreadyNotified.setNotified(true);

        Order wrongStatus = new Order();
        wrongStatus.setStatus(OrderStatus.PAID);

        when(orderRepository.findByClient_Email(eq(clientEmail), any()))
                .thenReturn(new PageImpl<>(List.of(order, alreadyNotified, wrongStatus)));

        List<OrderDTO> result = orderService.getNewShippedOrders(clientEmail);
        assertEquals(1, result.size());
    }

}