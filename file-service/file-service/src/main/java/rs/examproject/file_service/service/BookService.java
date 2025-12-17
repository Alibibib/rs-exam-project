package rs.examproject.file_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.examproject.file_service.dto.BookRequest;
import rs.examproject.file_service.dto.BookResponse;
import rs.examproject.file_service.model.Book;
import rs.examproject.file_service.repository.BookRepository;

import java.time.Duration;
import java.util.List;

@Service
public class BookService {

    private final BookRepository repo;
    private final RedisTemplate<String, Object> redis;
    private final Duration ttl;

    public BookService(BookRepository repo,
                       RedisTemplate<String, Object> redis,
                       @Value("${books.cache.ttl-seconds:30}") long ttlSeconds) {
        this.repo = repo;
        this.redis = redis;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    private String keyAll() { return "books:all"; }
    private String keyOne(long id) { return "books:id:" + id; }

    // GET -> кешируем список на 30 сек
    public List<BookResponse> getAll() {
        Object cached = redis.opsForValue().get(keyAll());
        if (cached instanceof List<?> list) {
            @SuppressWarnings("unchecked")
            List<BookResponse> casted = (List<BookResponse>) list;
            return casted;
        }

        List<BookResponse> fresh = repo.findAll().stream().map(this::toResp).toList();
        redis.opsForValue().set(keyAll(), fresh, ttl);
        return fresh;
    }

    // GET -> кешируем одну книгу на 30 сек
    public BookResponse getById(long id) {
        Object cached = redis.opsForValue().get(keyOne(id));
        if (cached instanceof BookResponse br) return br;

        Book book = repo.findById(id).orElseThrow(() -> new RuntimeException("Book not found: " + id));
        BookResponse fresh = toResp(book);
        redis.opsForValue().set(keyOne(id), fresh, ttl);
        return fresh;
    }

    // POST -> обычный, но сбрасываем кеш списка
    @Transactional
    public BookResponse create(BookRequest req) {
        Book b = new Book(req.title(), req.author(), req.isbn());
        Book saved = repo.save(b);

        redis.delete(keyAll()); // список устарел
        return toResp(saved);
    }

    // PUT -> обновляем в БД + кладём обновлённую запись в кеш на 30 сек
    @Transactional
    public BookResponse update(long id, BookRequest req) {
        Book b = repo.findById(id).orElseThrow(() -> new RuntimeException("Book not found: " + id));

        b.setTitle(req.title());
        b.setAuthor(req.author());
        b.setIsbn(req.isbn());

        Book saved = repo.save(b);
        BookResponse resp = toResp(saved);

        redis.opsForValue().set(keyOne(id), resp, ttl); // кешируем PUT
        redis.delete(keyAll());                         // список меняется
        return resp;
    }

    // DELETE -> удаляем из БД + чистим кеш
    @Transactional
    public void delete(long id) {
        repo.deleteById(id);
        redis.delete(keyOne(id));
        redis.delete(keyAll());
    }

    private BookResponse toResp(Book b) {
        return new BookResponse(b.getId(), b.getTitle(), b.getAuthor(), b.getIsbn(), b.getCreatedAt());
    }
}
