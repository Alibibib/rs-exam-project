package rs.examproject.file_service.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.examproject.file_service.dto.BookRequest;
import rs.examproject.file_service.dto.BookResponse;
import rs.examproject.file_service.service.BookService;

import java.util.List;

@RestController
@RequestMapping("/books")
public class BookController {

    private final BookService service;

    public BookController(BookService service) {
        this.service = service;
    }

    @GetMapping
    public List<BookResponse> all() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public BookResponse one(@PathVariable long id) {
        return service.getById(id);
    }

    @PostMapping
    public BookResponse create(@Valid @RequestBody BookRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public BookResponse update(@PathVariable long id, @Valid @RequestBody BookRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
