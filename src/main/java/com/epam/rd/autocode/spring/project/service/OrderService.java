package com.epam.rd.autocode.spring.project.service;

import com.epam.rd.autocode.spring.project.dto.OrderDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface OrderService {

    Page<OrderDTO> getOrdersByClient(String clientEmail, Pageable pageable);

    Page<OrderDTO> getOrdersByEmployee(String employeeEmail, Pageable pageable);

    Page<OrderDTO> getEmployeeDashboardOrders(String employeeEmail, Pageable pageable);

    OrderDTO addOrder(OrderDTO orderDTO);

    void processCheckout(String email);

    List<OrderDTO> getAllPaidOrders();

    void shipOrder(Long id, String employeeEmail);

    List<OrderDTO> getNewShippedOrders(String clientEmail);

    Page<OrderDTO> getAllOrders(String search, Pageable pageable);
}