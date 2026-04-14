package com.epam.rd.autocode.spring.project.service.impl;

import com.epam.rd.autocode.spring.project.model.CartItem;
import com.epam.rd.autocode.spring.project.repo.CartRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    void testGetCartItems() {
        String email = "test@example.com";
        CartItem item1 = new CartItem();
        CartItem item2 = new CartItem();
        when(cartRepository.findByClientEmail(email)).thenReturn(Arrays.asList(item1, item2));

        List<CartItem> result = cartService.getCartItems(email);

        assertEquals(2, result.size());
        verify(cartRepository, times(1)).findByClientEmail(email);
    }

    @Test
    void testGetCartTotal() {
        String email = "test@example.com";
        CartItem item1 = mock(CartItem.class);
        CartItem item2 = mock(CartItem.class);

        when(item1.getTotalPrice()).thenReturn(new BigDecimal("10.50"));
        when(item2.getTotalPrice()).thenReturn(new BigDecimal("20.00"));
        when(cartRepository.findByClientEmail(email)).thenReturn(Arrays.asList(item1, item2));

        BigDecimal total = cartService.getCartTotal(email);

        assertEquals(new BigDecimal("30.50"), total);
    }

    @Test
    void testAddItemToDatabaseCart_NewItem() {
        String email = "test@example.com";
        String bookName = "Java Concurrency";
        int quantity = 2;
        BigDecimal price = new BigDecimal("15.00");

        when(cartRepository.findByClientEmailAndBookName(email, bookName)).thenReturn(null);

        cartService.addItemToDatabaseCart(email, bookName, quantity, price);

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartRepository, times(1)).save(captor.capture());

        CartItem savedItem = captor.getValue();
        assertEquals(email, savedItem.getClientEmail());
        assertEquals(bookName, savedItem.getBookName());
        assertEquals(quantity, savedItem.getQuantity());
        assertEquals(price, savedItem.getPrice());
    }

    @Test
    void testAddItemToDatabaseCart_ExistingItem() {
        String email = "test@example.com";
        String bookName = "Java Concurrency";
        int extraQuantity = 2;
        BigDecimal price = new BigDecimal("15.00");

        CartItem existingItem = new CartItem();
        existingItem.setClientEmail(email);
        existingItem.setBookName(bookName);
        existingItem.setQuantity(3); // Уже есть 3 штуки
        existingItem.setPrice(price);

        when(cartRepository.findByClientEmailAndBookName(email, bookName)).thenReturn(existingItem);

        cartService.addItemToDatabaseCart(email, bookName, extraQuantity, price);

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartRepository, times(1)).save(captor.capture());

        CartItem savedItem = captor.getValue();
        assertEquals(5, savedItem.getQuantity());
    }

    @Test
    void testRemoveItemFromDatabaseCart_ItemExists() {
        String email = "test@example.com";
        String bookName = "Spring in Action";
        CartItem existingItem = new CartItem();

        when(cartRepository.findByClientEmailAndBookName(email, bookName)).thenReturn(existingItem);

        cartService.removeItemFromDatabaseCart(email, bookName);

        verify(cartRepository, times(1)).delete(existingItem);
    }

    @Test
    void testRemoveItemFromDatabaseCart_ItemDoesNotExist() {
        String email = "test@example.com";
        String bookName = "Spring in Action";

        when(cartRepository.findByClientEmailAndBookName(email, bookName)).thenReturn(null);

        cartService.removeItemFromDatabaseCart(email, bookName);

        verify(cartRepository, never()).delete(any());
    }
}