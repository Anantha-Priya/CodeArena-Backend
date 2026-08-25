package com.codearena.controller;

import com.codearena.dto.ProblemRequest;
import com.codearena.dto.ProblemResponse;
import com.codearena.entity.Difficulty;
import com.codearena.service.ProblemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;

    @PostMapping
    public ResponseEntity<ProblemResponse> create(@Valid @RequestBody ProblemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(problemService.create(request));
    }

    @GetMapping
    public ResponseEntity<Page<ProblemResponse>> search(
        @RequestParam(required = false) Difficulty difficulty,
        @RequestParam(required = false) String topic,
        Pageable pageable
    ) {
        return ResponseEntity.ok(problemService.search(difficulty, topic, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProblemResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(problemService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProblemResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody ProblemRequest request
    ) {
        return ResponseEntity.ok(problemService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        problemService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
