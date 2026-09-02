package com.codearena.service;

import com.codearena.dto.LeaderboardEntryResponse;
import com.codearena.entity.Contest;
import com.codearena.entity.ContestParticipant;
import com.codearena.entity.Submission;
import com.codearena.entity.SubmissionStatus;
import com.codearena.entity.User;
import com.codearena.exception.ResourceNotFoundException;
import com.codearena.repository.ContestParticipantRepository;
import com.codearena.repository.ContestRepository;
import com.codearena.repository.SubmissionRepository;
import com.codearena.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock
    private ContestRepository contestRepository;
    @Mock
    private ContestParticipantRepository contestParticipantRepository;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private UserRepository userRepository;

    private LeaderboardService service;

    private User alice;
    private User bob;
    private User carol;
    private User dave;
    private Contest activeContest;
    private Contest endedContest;

    @BeforeEach
    void setUp() {
        service = new LeaderboardService(
            contestRepository, contestParticipantRepository, submissionRepository, userRepository
        );
        alice = User.builder().id(1L).username("alice").rating(0).build();
        bob = User.builder().id(2L).username("bob").rating(0).build();
        carol = User.builder().id(3L).username("carol").rating(0).build();
        dave = User.builder().id(4L).username("dave").rating(0).build();
        activeContest = Contest.builder()
            .id(1L)
            .startTime(Instant.now().minus(1, ChronoUnit.MINUTES))
            .endTime(Instant.now().plus(1, ChronoUnit.HOURS))
            .build();
        endedContest = Contest.builder()
            .id(2L)
            .startTime(Instant.now().minus(2, ChronoUnit.HOURS))
            .endTime(Instant.now().minus(1, ChronoUnit.HOURS))
            .build();
    }

    private ContestParticipant participant(User user, Contest contest) {
        return ContestParticipant.builder().user(user).contest(contest).ratingApplied(false).build();
    }

    private Submission accepted(User user, Contest contest, int score) {
        return Submission.builder().user(user).contest(contest).status(SubmissionStatus.ACCEPTED).score(score).build();
    }

    @Test
    void contestNotFoundThrows404() {
        when(contestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLeaderboard(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void ranksParticipantsByTotalAcceptedScoreDescending() {
        when(contestRepository.findById(1L)).thenReturn(Optional.of(activeContest));
        when(contestParticipantRepository.findByContestId(1L)).thenReturn(List.of(
            participant(alice, activeContest), participant(bob, activeContest)
        ));
        when(submissionRepository.findByContestIdAndStatus(1L, SubmissionStatus.ACCEPTED)).thenReturn(List.of(
            accepted(alice, activeContest, 100),
            accepted(bob, activeContest, 300)
        ));

        List<LeaderboardEntryResponse> leaderboard = service.getLeaderboard(1L);

        assertThat(leaderboard).hasSize(2);
        assertThat(leaderboard.get(0).getUsername()).isEqualTo("bob");
        assertThat(leaderboard.get(0).getRank()).isEqualTo(1);
        assertThat(leaderboard.get(0).getScore()).isEqualTo(300);
        assertThat(leaderboard.get(1).getUsername()).isEqualTo("alice");
        assertThat(leaderboard.get(1).getRank()).isEqualTo(2);
        assertThat(leaderboard.get(1).getScore()).isEqualTo(100);
    }

    @Test
    void sumsMultipleAcceptedSubmissionsPerUser() {
        when(contestRepository.findById(1L)).thenReturn(Optional.of(activeContest));
        when(contestParticipantRepository.findByContestId(1L)).thenReturn(List.of(participant(alice, activeContest)));
        when(submissionRepository.findByContestIdAndStatus(1L, SubmissionStatus.ACCEPTED)).thenReturn(List.of(
            accepted(alice, activeContest, 100),
            accepted(alice, activeContest, 200)
        ));

        List<LeaderboardEntryResponse> leaderboard = service.getLeaderboard(1L);

        assertThat(leaderboard.get(0).getScore()).isEqualTo(300);
    }

    @Test
    void doesNotApplyRatingWhileContestIsActive() {
        when(contestRepository.findById(1L)).thenReturn(Optional.of(activeContest));
        when(contestParticipantRepository.findByContestId(1L)).thenReturn(List.of(participant(alice, activeContest)));
        when(submissionRepository.findByContestIdAndStatus(1L, SubmissionStatus.ACCEPTED)).thenReturn(List.of());

        service.getLeaderboard(1L);

        verify(userRepository, never()).save(any());
        verify(contestParticipantRepository, never()).save(any());
    }

    @Test
    void appliesParticipationAndTop3BonusWhenContestEnded() {
        when(contestRepository.findById(2L)).thenReturn(Optional.of(endedContest));
        when(contestParticipantRepository.findByContestId(2L)).thenReturn(List.of(
            participant(alice, endedContest), participant(bob, endedContest),
            participant(carol, endedContest), participant(dave, endedContest)
        ));
        when(submissionRepository.findByContestIdAndStatus(2L, SubmissionStatus.ACCEPTED)).thenReturn(List.of(
            accepted(bob, endedContest, 300),
            accepted(alice, endedContest, 200),
            accepted(carol, endedContest, 100)
            // dave: no accepted submissions -> score 0, rank 4
        ));

        service.getLeaderboard(2L);

        // rank 1 bob, rank 2 alice, rank 3 carol: +10 +50 = 60; rank 4 dave: +10 only
        assertThat(bob.getRating()).isEqualTo(60);
        assertThat(alice.getRating()).isEqualTo(60);
        assertThat(carol.getRating()).isEqualTo(60);
        assertThat(dave.getRating()).isEqualTo(10);
        verify(userRepository, times(4)).save(any());
    }

    @Test
    void ratingBonusIsNotReappliedOnASecondLeaderboardView() {
        ContestParticipant aliceParticipation = participant(alice, endedContest);
        aliceParticipation.setRatingApplied(true);
        alice.setRating(60);

        when(contestRepository.findById(2L)).thenReturn(Optional.of(endedContest));
        when(contestParticipantRepository.findByContestId(2L)).thenReturn(List.of(aliceParticipation));
        when(submissionRepository.findByContestIdAndStatus(2L, SubmissionStatus.ACCEPTED)).thenReturn(List.of(
            accepted(alice, endedContest, 200)
        ));

        service.getLeaderboard(2L);

        assertThat(alice.getRating()).isEqualTo(60);
        verify(userRepository, never()).save(any());
        verify(contestParticipantRepository, never()).save(any());
    }

    @Test
    void sweepAppliesBonusesForEndedContestsNobodyHasViewed() {
        when(contestRepository.findAll()).thenReturn(List.of(activeContest, endedContest));
        when(contestParticipantRepository.findByContestId(2L)).thenReturn(List.of(
            participant(alice, endedContest), participant(bob, endedContest),
            participant(carol, endedContest), participant(dave, endedContest)
        ));
        when(submissionRepository.findByContestIdAndStatus(2L, SubmissionStatus.ACCEPTED)).thenReturn(List.of(
            accepted(bob, endedContest, 300),
            accepted(alice, endedContest, 200),
            accepted(carol, endedContest, 100)
            // dave: no accepted submissions -> score 0, rank 4
        ));

        service.applyBonusesForEndedContests();

        // rank 1 bob, rank 2 alice, rank 3 carol: +10 +50 = 60; rank 4 dave: +10 only.
        // activeContest is never touched.
        assertThat(bob.getRating()).isEqualTo(60);
        assertThat(alice.getRating()).isEqualTo(60);
        assertThat(carol.getRating()).isEqualTo(60);
        assertThat(dave.getRating()).isEqualTo(10);
        verify(submissionRepository, never()).findByContestIdAndStatus(1L, SubmissionStatus.ACCEPTED);
    }

    @Test
    void sweepSkipsParticipantsAlreadyBonused() {
        ContestParticipant aliceParticipation = participant(alice, endedContest);
        aliceParticipation.setRatingApplied(true);
        alice.setRating(60);

        when(contestRepository.findAll()).thenReturn(List.of(endedContest));
        when(contestParticipantRepository.findByContestId(2L)).thenReturn(List.of(aliceParticipation));
        when(submissionRepository.findByContestIdAndStatus(2L, SubmissionStatus.ACCEPTED)).thenReturn(List.of(
            accepted(alice, endedContest, 200)
        ));

        service.applyBonusesForEndedContests();

        assertThat(alice.getRating()).isEqualTo(60);
        verify(userRepository, never()).save(any());
        verify(contestParticipantRepository, never()).save(any());
    }

}
