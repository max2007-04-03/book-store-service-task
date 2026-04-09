package com.epam.rd.autocode.spring.project.repo;

import com.epam.rd.autocode.spring.project.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByClient_Email(String email, Pageable pageable);

    Page<Order> findByEmployee_Email(String email, Pageable pageable);

    @Query("SELECT o FROM Order o LEFT JOIN o.employee e " +
            "WHERE (e.email = :email) OR (o.status = com.epam.rd.autocode.spring.project.model.enums.OrderStatus.PAID)")
    Page<Order> findAllForEmployeeDashboard(@Param("email") String email, Pageable pageable);
}