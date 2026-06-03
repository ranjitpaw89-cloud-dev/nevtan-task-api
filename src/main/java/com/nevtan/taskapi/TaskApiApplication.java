package com.nevtan.taskapi;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@SpringBootApplication
public class TaskApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(TaskApiApplication.class, args);
    }
}

@RestController
class RootController {

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of("service", "nevtan-task-api", "status", "ok", "version", "1.0.0");
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}

@RestController
@RequestMapping("/api/tasks")
class TaskController {

    private final Map<Long, Task> store = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(0);

    @GetMapping
    public Collection<Task> list() {
        return store.values();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> get(@PathVariable Long id) {
        Task t = store.get(id);
        if (t == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(t);
    }

    @PostMapping
    public ResponseEntity<Task> create(@Valid @RequestBody TaskRequest req) {
        long id = seq.incrementAndGet();
        boolean completed = req.completed() != null && req.completed();
        Task t = new Task(id, req.title(), completed, Instant.now().toString());
        store.put(id, t);
        return ResponseEntity.status(HttpStatus.CREATED).body(t);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> update(@PathVariable Long id, @Valid @RequestBody TaskRequest req) {
        Task existing = store.get(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        boolean completed = req.completed() != null && req.completed();
        Task updated = new Task(id, req.title(), completed, existing.createdAt());
        store.put(id, updated);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (store.remove(id) == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}

record Task(Long id, String title, boolean completed, String createdAt) {
}

record TaskRequest(@NotBlank String title, Boolean completed) {
}
