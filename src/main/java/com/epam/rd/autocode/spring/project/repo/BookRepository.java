package com.epam.rd.autocode.spring.project.repo;

import com.epam.rd.autocode.spring.project.model.Book;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByName(String name);

    void deleteByName(String name);

    boolean existsByName(String name);

    Page<Book> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Book> findByGenre(String genre, Pageable pageable);
}