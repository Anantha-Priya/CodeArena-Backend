package com.codearena.service;

import com.codearena.entity.Difficulty;
import com.codearena.entity.SubmissionStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScoreServiceTest {

    private final ScoreService scoreService = new ScoreService();

    @Test
    void acceptedEasyScores100() {
        assertThat(scoreService.calculateScore(SubmissionStatus.ACCEPTED, Difficulty.EASY)).isEqualTo(100);
    }

    @Test
    void acceptedMediumScores200() {
        assertThat(scoreService.calculateScore(SubmissionStatus.ACCEPTED, Difficulty.MEDIUM)).isEqualTo(200);
    }

    @Test
    void acceptedHardScores300() {
        assertThat(scoreService.calculateScore(SubmissionStatus.ACCEPTED, Difficulty.HARD)).isEqualTo(300);
    }

    @Test
    void wrongAnswerScoresZeroRegardlessOfDifficulty() {
        assertThat(scoreService.calculateScore(SubmissionStatus.WRONG_ANSWER, Difficulty.HARD)).isZero();
    }

    @Test
    void compilationErrorScoresZero() {
        assertThat(scoreService.calculateScore(SubmissionStatus.COMPILATION_ERROR, Difficulty.MEDIUM)).isZero();
    }

}
