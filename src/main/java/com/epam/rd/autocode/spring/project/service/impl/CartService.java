package com.epam.rd.autocode.spring.project.service.impl;

import com.epam.rd.autocode.spring.project.model.CartItem;
import com.epam.rd.autocode.spring.project.repo.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;

    public List<CartItem> getCartItems(String email) {
        return cartRepository.findByClientEmail(email);
    }

    public BigDecimal getCartTotal(String email) {
        return getCartItems(email).stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public void addItemToDatabaseCart(String email, String bookName, int quantity, BigDecimal price) {
        CartItem item = cartRepository.findByClientEmailAndBookName(email, bookName);

        if (item == null) {
            item = new CartItem();
            item.setClientEmail(email);
            item.setBookName(bookName);
            item.setPrice(price);
            item.setQuantity(quantity);
        } else {
            item.setQuantity(item.getQuantity() + quantity);
        }
        cartRepository.save(item);
    }

    @Transactional
    public void removeItemFromDatabaseCart(String email, String bookName) {
        CartItem item = cartRepository.findByClientEmailAndBookName(email, bookName);
        if (item != null) {
            cartRepository.delete(item);
        }
    }
}