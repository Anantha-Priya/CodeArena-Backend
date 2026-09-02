package com.codearena.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContestResponse {

    private Long id;
    private String title;
    private String description;

    // Explicit format so this always serializes with a trailing Z (ISO_INSTANT), regardless
    // of the app's configured Jackson time zone - see API_REFERENCE.md's documented contract.
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    private Instant startTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    private Instant endTime;
    private LocalDateTime createdAt;

}
