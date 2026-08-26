package com.codearena.dto;

import com.codearena.entity.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionResponse {

    private Long id;
    private Long problemId;
    private String problemTitle;
    private Long contestId;
    private String contestTitle;
    private String language;
    private SubmissionStatus status;
    private int score;
    private LocalDateTime submittedAt;

}
