package com.codearena.dto;

import com.codearena.entity.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProblemRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotNull
    private Difficulty difficulty;

    @NotBlank
    private String topic;

    @NotBlank
    private String constraints;

    @NotBlank
    private String inputFormat;

    @NotBlank
    private String outputFormat;

    @NotBlank
    private String sampleInput;

    @NotBlank
    private String sampleOutput;

}
