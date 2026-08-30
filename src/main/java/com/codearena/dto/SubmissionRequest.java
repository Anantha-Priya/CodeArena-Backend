package com.codearena.dto;

import com.codearena.entity.SubmissionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * status is supplied by the caller, not computed by a judge — v1 doesn't execute code
 * (see CLAUDE.md); it's persisted as data and ScoreService derives the score from it.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionRequest {

    /** Omit for a practice submission (not tied to any contest). */
    private Long contestId;

    @NotNull
    private Long problemId;

    @NotBlank
    private String language;

    @NotBlank
    private String sourceCode;

    @NotNull
    private SubmissionStatus status;

}
