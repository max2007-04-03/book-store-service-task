package com.epam.rd.autocode.spring.project.repo;


import com.epam.rd.autocode.spring.project.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByClient_Email(String email);

    List<Order> findByEmployee_Email(String email);


}
