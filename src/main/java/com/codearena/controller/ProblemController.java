package com.codearena.controller;

import com.codearena.dto.ProblemRequest;
import com.codearena.dto.ProblemResponse;
import com.codearena.entity.Difficulty;
import com.codearena.exception.ErrorResponse;
import com.codearena.service.ProblemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;

    @Operation(
        summary = "Create a problem (admin only)",
        tags = {"Problems"},
        responses = {
            @ApiResponse(responseCode = "201", description = "Created",
                content = @Content(schema = @Schema(implementation = ProblemResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing/invalid token",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not an admin",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @PostMapping
    public ResponseEntity<ProblemResponse> create(@Valid @RequestBody ProblemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(problemService.create(request));
    }

    @Operation(
        summary = "Search problems",
        description = "difficulty and topic are both optional and independent; page/size follow Spring's standard Pageable query params.",
        tags = {"Problems"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Filtered, paginated results"),
            @ApiResponse(responseCode = "401", description = "Missing/invalid token",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @GetMapping
    public ResponseEntity<Page<ProblemResponse>> search(
        @Parameter(description = "Optional exact-match filter") @RequestParam(required = false) Difficulty difficulty,
        @Parameter(description = "Optional exact-match filter") @RequestParam(required = false) String topic,
        Pageable pageable
    ) {
        return ResponseEntity.ok(problemService.search(difficulty, topic, pageable));
    }

    @Operation(
        summary = "Get a problem by id",
        tags = {"Problems"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Found",
                content = @Content(schema = @Schema(implementation = ProblemResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing/invalid token",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No problem with that id",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @GetMapping("/{id}")
    public ResponseEntity<ProblemResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(problemService.getById(id));
    }

    @Operation(
        summary = "Replace a problem (admin only)",
        tags = {"Problems"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Updated",
                content = @Content(schema = @Schema(implementation = ProblemResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing/invalid token",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not an admin",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No problem with that id",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @PutMapping("/{id}")
    public ResponseEntity<ProblemResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody ProblemRequest request
    ) {
        return ResponseEntity.ok(problemService.update(id, request));
    }

    @Operation(
        summary = "Delete a problem (admin only)",
        tags = {"Problems"},
        responses = {
            @ApiResponse(responseCode = "204", description = "Deleted, no response body"),
            @ApiResponse(responseCode = "401", description = "Missing/invalid token",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not an admin",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No problem with that id",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        problemService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
