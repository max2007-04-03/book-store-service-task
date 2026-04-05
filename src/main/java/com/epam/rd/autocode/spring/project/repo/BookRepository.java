package com.epam.rd.autocode.spring.project.repo;

import com.epam.rd.autocode.spring.project.model.Book;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByName(String name);

    void deleteByName(String name);


}
