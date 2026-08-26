package com.codearena.service;

import com.codearena.entity.Difficulty;
import com.codearena.entity.SubmissionStatus;
import org.springframework.stereotype.Service;

@Service
public class ScoreService {

    public int calculateScore(SubmissionStatus status, Difficulty difficulty) {
        if (status != SubmissionStatus.ACCEPTED) {
            return 0;
        }

        return switch (difficulty) {
            case EASY -> 100;
            case MEDIUM -> 200;
            case HARD -> 300;
        };
    }

}
