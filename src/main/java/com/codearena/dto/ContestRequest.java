package com.codearena.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EndTimeAfterStartTime
public class ContestRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    /** ISO-8601 instant, e.g. "2026-01-01T04:30:00Z" - not a naive local time. */
    @NotNull
    private Instant startTime;

    @NotNull
    private Instant endTime;

}
