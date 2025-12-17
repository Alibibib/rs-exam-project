package rs.examproject.file_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.examproject.file_service.model.Book;

public interface BookRepository extends JpaRepository<Book, Long> {
}
