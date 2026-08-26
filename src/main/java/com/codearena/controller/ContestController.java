package com.codearena.controller;

import com.codearena.dto.ContestRequest;
import com.codearena.dto.ContestResponse;
import com.codearena.dto.ContestStatusResponse;
import com.codearena.service.ContestParticipantService;
import com.codearena.service.ContestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/contests")
@RequiredArgsConstructor
public class ContestController {

    private final ContestService contestService;
    private final ContestParticipantService contestParticipantService;

    @PostMapping
    public ResponseEntity<ContestResponse> create(@Valid @RequestBody ContestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contestService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<ContestResponse>> getAll() {
        return ResponseEntity.ok(contestService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContestResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(contestService.getById(id));
    }

    @PostMapping("/{contestId}/problems/{problemId}")
    public ResponseEntity<Void> addProblem(@PathVariable Long contestId, @PathVariable Long problemId) {
        contestService.addProblemToContest(contestId, problemId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<ContestStatusResponse> getStatus(@PathVariable Long id) {
        return ResponseEntity.ok(contestService.getStatus(id));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<Void> join(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        contestParticipantService.join(id, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
