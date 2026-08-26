package com.codearena.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ContestTest {

    @Test
    void upcomingBeforeStartTime() {
        Contest contest = Contest.builder()
            .startTime(LocalDateTime.now().plusHours(1))
            .endTime(LocalDateTime.now().plusHours(2))
            .build();

        assertThat(contest.getStatus()).isEqualTo(ContestStatus.UPCOMING);
    }

    @Test
    void activeBetweenStartAndEndTime() {
        Contest contest = Contest.builder()
            .startTime(LocalDateTime.now().minusMinutes(1))
            .endTime(LocalDateTime.now().plusHours(1))
            .build();

        assertThat(contest.getStatus()).isEqualTo(ContestStatus.ACTIVE);
    }

    @Test
    void endedAfterEndTime() {
        Contest contest = Contest.builder()
            .startTime(LocalDateTime.now().minusHours(2))
            .endTime(LocalDateTime.now().minusHours(1))
            .build();

        assertThat(contest.getStatus()).isEqualTo(ContestStatus.ENDED);
    }

    @Test
    void endedExactlyAtEndTime() {
        LocalDateTime end = LocalDateTime.now().minusSeconds(1);
        Contest contest = Contest.builder()
            .startTime(end.minusHours(1))
            .endTime(end)
            .build();

        // now >= end_time (guide spec: ACTIVE is start <= now < end, so "now" past end is ENDED)
        assertThat(contest.getStatus()).isEqualTo(ContestStatus.ENDED);
    }

}
