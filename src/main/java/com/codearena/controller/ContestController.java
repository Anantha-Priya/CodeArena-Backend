package com.codearena.controller;

import com.codearena.dto.ContestRequest;
import com.codearena.dto.ContestResponse;
import com.codearena.dto.ContestStatusResponse;
import com.codearena.dto.LeaderboardEntryResponse;
import com.codearena.dto.ProblemResponse;
import com.codearena.exception.ErrorResponse;
import com.codearena.service.ContestParticipantService;
import com.codearena.service.ContestService;
import com.codearena.service.LeaderboardService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/contests")
@RequiredArgsConstructor
public class ContestController {

    private final ContestService contestService;
    private final ContestParticipantService contestParticipantService;
    private final LeaderboardService leaderboardService;

    @Operation(
        summary = "Create a contest (admin only)",
        description = "end_time must be after start_time.",
        tags = {"Contests"},
        responses = {
            @ApiResponse(responseCode = "201", description = "Created",
                content = @Content(schema = @Schema(implementation = ContestResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed (including end_time <= start_time)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing/invalid token",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not an admin",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @PostMapping
    public ResponseEntity<ContestResponse> create(@Valid @RequestBody ContestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contestService.create(request));
    }

    @Operation(
        summary = "List all contests",
        tags = {"Contests"},
        responses = {
            @ApiResponse(responseCode = "200", description = "All contests, not paginated"),
            @ApiResponse(responseCode = "401", description = "Missing/invalid token",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @GetMapping
    public ResponseEntity<List<ContestResponse>> getAll() {
        return ResponseEntity.ok(contestService.getAll());
    }

    @Operation(
        summary = "Get a contest by id",
        tags = {"Contests"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Found",
                content = @Content(schema = @Schema(implementation = ContestResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing/invalid token",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No contest with that id",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @GetMapping("/{id}")
    public ResponseEntity<ContestResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(contestService.getById(id));
    }

    @Operation(
        summary = "Associate a problem with a contest (admin only)",
        tags = {"Contests"},
        responses = {
            @ApiResponse(responseCode = "201", description = "Associated, no response body"),
            @ApiResponse(responseCode = "401", description = "Missing/invalid token",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not an admin",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No contest or no problem with that id",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Problem is already associated with this contest",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @PostMapping("/{contestId}/problems/{problemId}")
    public ResponseEntity<Void> addProblem(@PathVariable Long contestId, @PathVariable Long problemId) {
        contestService.addProblemToContest(contestId, problemId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(
        summary = "Get the problems attached to a contest",
        description = "Same ProblemResponse shape as GET /api/problems/{id}. Returns an empty "
            + "array (not 404) if the contest exists but has no problems attached yet.",
        tags = {"Contests"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Attached problems (possibly empty)",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProblemResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Missing/invalid token",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No contest with that id",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @GetMapping("/{id}/problems")
    public ResponseEntity<List<ProblemResponse>> getProblems(@PathVariable Long id) {
        return ResponseEntity.ok(contestService.getProblemsForContest(id));
    }

    @Operation(
        summary = "Get a contest's live status",
        description = "Computed purely from start_time/end_time vs. server time - UPCOMING, ACTIVE, or ENDED.",
        tags = {"Contests"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Current status and seconds remaining",
                content = @Content(schema = @Schema(implementation = ContestStatusResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing/invalid token",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No contest with that id",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @GetMapping("/{id}/status")
    public ResponseEntity<ContestStatusResponse> getStatus(@PathVariable Long id) {
        return ResponseEntity.ok(contestService.getStatus(id));
    }

    @Operation(
        summary = "Join a contest",
        description = "Allowed for UPCOMING and ACTIVE contests, rejected once a contest has ENDED.",
        tags = {"Contests"},
        responses = {
            @ApiResponse(responseCode = "201", description = "Joined, no response body"),
            @ApiResponse(responseCode = "400", description = "Contest has already ended",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing/invalid token",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No contest with that id",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Already joined this contest",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @PostMapping("/{id}/join")
    public ResponseEntity<Void> join(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        contestParticipantService.join(id, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(
        summary = "Get a contest's leaderboard",
        description = "Participants ranked by summed score from their ACCEPTED submissions, descending. "
            + "Once the contest has ENDED, viewing this also applies the rating bump "
            + "(+10 participating, +50 more for top 3) exactly once per participant.",
        tags = {"Leaderboard"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Ranked entries",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = LeaderboardEntryResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Missing/invalid token",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No contest with that id",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @GetMapping("/{id}/leaderboard")
    public ResponseEntity<List<LeaderboardEntryResponse>> getLeaderboard(@PathVariable Long id) {
        return ResponseEntity.ok(leaderboardService.getLeaderboard(id));
    }

}
