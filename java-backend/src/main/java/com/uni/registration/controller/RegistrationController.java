package com.uni.registration.controller;

import com.uni.registration.dto.RegistrationRequest;
import com.uni.registration.dto.RegistrationResponse;
import com.uni.registration.exception.RegistrationException;
import com.uni.registration.service.RegistrationService;
import com.uni.registration.service.concurrent.RegistrationExecutor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/registrations")
public class RegistrationController {

    private final RegistrationService service;
    private final RegistrationExecutor executor;

    public RegistrationController(RegistrationService service, RegistrationExecutor executor) {
        this.service = service;
        this.executor = executor;
    }

    @PostMapping
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegistrationRequest req)
            throws RegistrationException {
        return ResponseEntity.ok(service.register(req.getStudentId(), req.getCourseId()));
    }

    /**
     * Concurrent endpoint — accepts a batch and dispatches them to the thread pool.
     * Useful for the front-end when a student registers for many courses at once.
     */
    @PostMapping("/batch")
    public CompletableFuture<List<RegistrationResponse>> batch(@RequestBody List<RegistrationRequest> reqs) {
        List<CompletableFuture<RegistrationResponse>> futures = reqs.stream()
                .map(r -> executor.submit(r.getStudentId(), r.getCourseId()))
                .collect(Collectors.toList());
        return CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream().map(CompletableFuture::join).collect(Collectors.toList()));
    }

    @DeleteMapping("/{studentId}/courses/{courseId}")
    public ResponseEntity<Void> drop(@PathVariable String studentId, @PathVariable String courseId) {
        boolean dropped = service.drop(studentId, courseId);
        return dropped ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/{studentId}/undo")
    public ResponseEntity<Void> undo(@PathVariable String studentId) {
        boolean ok = service.undoLast(studentId);
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
