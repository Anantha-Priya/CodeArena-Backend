package com.codearena.controller;

import com.codearena.dto.SubmissionRequest;
import com.codearena.dto.SubmissionResponse;
import com.codearena.exception.ErrorResponse;
import com.codearena.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @Operation(
        summary = "Submit a solution",
        description = "Validation order: contest exists, problem exists, caller has joined the contest, "
            + "contest is ACTIVE, problem belongs to the contest. Score is computed by ScoreService "
            + "from the submitted status and the problem's difficulty.",
        tags = {"Submissions"},
        responses = {
            @ApiResponse(responseCode = "201", description = "Submission recorded and scored",
                content = @Content(schema = @Schema(implementation = SubmissionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed, not joined, contest not ACTIVE, "
                + "or problem not in this contest",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing/invalid token",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No contest or no problem with that id",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @PostMapping
    public ResponseEntity<SubmissionResponse> submit(
        @Valid @RequestBody SubmissionRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(submissionService.submit(request, userDetails.getUsername()));
    }

    @Operation(
        summary = "List the caller's own submissions",
        tags = {"Submissions"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Only the caller's own submissions",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = SubmissionResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Missing/invalid token",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @GetMapping("/my")
    public ResponseEntity<List<SubmissionResponse>> getMySubmissions(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(submissionService.getMySubmissions(userDetails.getUsername()));
    }

}
