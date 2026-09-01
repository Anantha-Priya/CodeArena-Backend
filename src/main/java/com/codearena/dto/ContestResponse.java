package com.codearena.dto;

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
    private Instant startTime;
    private Instant endTime;
    private LocalDateTime createdAt;

}
