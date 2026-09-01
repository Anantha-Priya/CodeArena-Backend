package com.codearena.entity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

class ContestTest {

    private final TimeZone originalDefaultTimeZone = TimeZone.getDefault();

    @AfterEach
    void restoreDefaultTimeZone() {
        TimeZone.setDefault(originalDefaultTimeZone);
    }

    @Test
    void upcomingBeforeStartTime() {
        Contest contest = Contest.builder()
            .startTime(Instant.now().plus(1, ChronoUnit.HOURS))
            .endTime(Instant.now().plus(2, ChronoUnit.HOURS))
            .build();

        assertThat(contest.getStatus()).isEqualTo(ContestStatus.UPCOMING);
    }

    @Test
    void activeBetweenStartAndEndTime() {
        Contest contest = Contest.builder()
            .startTime(Instant.now().minus(1, ChronoUnit.MINUTES))
            .endTime(Instant.now().plus(1, ChronoUnit.HOURS))
            .build();

        assertThat(contest.getStatus()).isEqualTo(ContestStatus.ACTIVE);
    }

    @Test
    void endedAfterEndTime() {
        Contest contest = Contest.builder()
            .startTime(Instant.now().minus(2, ChronoUnit.HOURS))
            .endTime(Instant.now().minus(1, ChronoUnit.HOURS))
            .build();

        assertThat(contest.getStatus()).isEqualTo(ContestStatus.ENDED);
    }

    @Test
    void endedExactlyAtEndTime() {
        Instant end = Instant.now().minusSeconds(1);
        Contest contest = Contest.builder()
            .startTime(end.minus(1, ChronoUnit.HOURS))
            .endTime(end)
            .build();

        // now >= end_time (guide spec: ACTIVE is start <= now < end, so "now" past end is ENDED)
        assertThat(contest.getStatus()).isEqualTo(ContestStatus.ENDED);
    }

    /**
     * The actual bug this migration fixes: with LocalDateTime, getStatus() compared the
     * server JVM's local wall clock against start/end values entered in a different zone
     * (e.g. an admin in IST, a server defaulting to UTC) - a contest that had genuinely
     * started could still read UPCOMING, off by exactly the zone offset. Instant has no
     * zone to disagree about, so the same absolute contest window must report the same
     * status no matter what timezone the JVM happens to be running in.
     */
    @Test
    void statusIsTheSameRegardlessOfJvmDefaultTimeZone() {
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(1, ChronoUnit.HOURS);
        Contest contest = Contest.builder().startTime(start).endTime(end).build();

        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        ContestStatus statusInUtc = contest.getStatus();

        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        ContestStatus statusInIst = contest.getStatus();

        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
        ContestStatus statusInLosAngeles = contest.getStatus();

        assertThat(statusInUtc).isEqualTo(ContestStatus.ACTIVE);
        assertThat(statusInIst).isEqualTo(ContestStatus.ACTIVE);
        assertThat(statusInLosAngeles).isEqualTo(ContestStatus.ACTIVE);
    }

}
