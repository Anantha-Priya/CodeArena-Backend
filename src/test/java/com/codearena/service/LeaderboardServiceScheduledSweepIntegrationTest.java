package com.codearena.service;

import com.codearena.entity.Contest;
import com.codearena.entity.ContestParticipant;
import com.codearena.entity.Role;
import com.codearena.entity.User;
import com.codearena.repository.ContestParticipantRepository;
import com.codearena.repository.ContestRepository;
import com.codearena.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduces applyBonusesForEndedContests() exactly as the real Spring scheduler invokes it:
 * no HTTP request in flight, so no open-in-view session, and no ambient transaction of any
 * kind. LeaderboardServiceTest (a pure Mockito unit test) cannot catch this class of bug at
 * all - mocked repositories hand back plain Java objects, never real Hibernate lazy proxies,
 * so there is nothing there to throw LazyInitializationException in the first place. This is
 * the same Propagation.NOT_SUPPORTED technique used for the analogous
 * detachProblemFromContest fix, applied here to a @Scheduled method instead of a controller
 * endpoint.
 */
@SpringBootTest
class LeaderboardServiceScheduledSweepIntegrationTest {

    @Autowired
    private LeaderboardService leaderboardService;

    @Autowired
    private ContestRepository contestRepository;

    @Autowired
    private ContestParticipantRepository contestParticipantRepository;

    @Autowired
    private UserRepository userRepository;

    private User saveUser(String slug) {
        return userRepository.save(User.builder()
            .username(slug)
            .email(slug + "@codearena.com")
            .password("x")
            .role(Role.USER)
            .build());
    }

    private Contest saveEndedContest(String slug) {
        return contestRepository.save(Contest.builder()
            .title(slug)
            .description("desc")
            .startTime(Instant.now().minusSeconds(7200))
            .endTime(Instant.now().minusSeconds(3600))
            .build());
    }

    /**
     * No submissions needed to reproduce this: applyRatingBonuses() touches
     * ContestParticipant.user (and thus the lazy User proxy) for every not-yet-rated
     * participant, even one with zero accepted submissions - it still gets the participation
     * bonus. Deliberately no ambient transaction (Propagation.NOT_SUPPORTED, no class-level
     * @Transactional to override) - this must run exactly like the real scheduler thread. No
     * auto-rollback, so cleanup is manual.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void scheduledSweepAppliesRatingBonusWithNoAmbientTransactionToMaskAMissingOne() {
        User user = saveUser("lb-it-notx-user");
        Contest contest = saveEndedContest("lb-it-notx-contest");
        ContestParticipant participant = contestParticipantRepository.save(
            ContestParticipant.builder().user(user).contest(contest).build()
        );

        try {
            leaderboardService.applyBonusesForEndedContests();

            User reloaded = userRepository.findById(user.getId()).orElseThrow();
            // Sole participant in an ended contest with no submissions: rank 1, so
            // participation (+10) and top-3 (+50) both apply.
            assertThat(reloaded.getRating()).isEqualTo(60);

            ContestParticipant reloadedParticipant = contestParticipantRepository.findById(participant.getId())
                .orElseThrow();
            assertThat(reloadedParticipant.isRatingApplied()).isTrue();
        } finally {
            contestParticipantRepository.deleteById(participant.getId());
            contestRepository.deleteById(contest.getId());
            userRepository.deleteById(user.getId());
        }
    }

}
