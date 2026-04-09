package com.epam.rd.autocode.spring.project.repo;

import com.epam.rd.autocode.spring.project.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CartRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByClientEmail(String clientEmail);


    CartItem findByClientEmailAndBookName(String clientEmail, String bookName);
}