package com.codearena.service;

import com.codearena.dto.LeaderboardEntryResponse;
import com.codearena.entity.Contest;
import com.codearena.entity.ContestParticipant;
import com.codearena.entity.ContestStatus;
import com.codearena.entity.Submission;
import com.codearena.entity.SubmissionStatus;
import com.codearena.entity.User;
import com.codearena.exception.ResourceNotFoundException;
import com.codearena.repository.ContestParticipantRepository;
import com.codearena.repository.ContestRepository;
import com.codearena.repository.SubmissionRepository;
import com.codearena.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private static final int PARTICIPATION_RATING_BONUS = 10;
    private static final int TOP_THREE_RATING_BONUS = 50;
    private static final int TOP_THREE_CUTOFF = 3;

    private final ContestRepository contestRepository;
    private final ContestParticipantRepository contestParticipantRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;

    public List<LeaderboardEntryResponse> getLeaderboard(Long contestId) {
        Contest contest = contestRepository.findById(contestId)
            .orElseThrow(() -> new ResourceNotFoundException("Contest not found: " + contestId));

        Map<Long, Integer> scoreByUserId = scoreByUserId(contestId);
        List<ContestParticipant> ranked = rankParticipants(contestId, scoreByUserId);

        if (contest.getStatus() == ContestStatus.ENDED) {
            applyRatingBonuses(ranked);
        }

        return buildResponse(ranked, scoreByUserId);
    }

    /**
     * Backstop for the lazy rating bump above: getLeaderboard() only applies it to a contest
     * someone actually views after it ends, so a contest nobody checks the leaderboard for
     * would otherwise sit at rating 0 forever. Runs on a fixed schedule instead of a
     * "finalize contest" event (there isn't one); applyRatingBonuses() is already idempotent
     * per participant (guarded by ratingApplied), so repeated sweeps are safe.
     */
    @Scheduled(fixedRate = 60_000, initialDelay = 30_000)
    public void applyBonusesForEndedContests() {
        contestRepository.findAll().stream()
            .filter(contest -> contest.getStatus() == ContestStatus.ENDED)
            .forEach(contest -> {
                Map<Long, Integer> scoreByUserId = scoreByUserId(contest.getId());
                applyRatingBonuses(rankParticipants(contest.getId(), scoreByUserId));
            });
    }

    private Map<Long, Integer> scoreByUserId(Long contestId) {
        return submissionRepository
            .findByContestIdAndStatus(contestId, SubmissionStatus.ACCEPTED).stream()
            .collect(Collectors.groupingBy(
                submission -> submission.getUser().getId(),
                Collectors.summingInt(Submission::getScore)
            ));
    }

    private List<ContestParticipant> rankParticipants(Long contestId, Map<Long, Integer> scoreByUserId) {
        List<ContestParticipant> participants = contestParticipantRepository.findByContestId(contestId);

        return participants.stream()
            .sorted(Comparator.comparingInt(
                (ContestParticipant p) -> scoreByUserId.getOrDefault(p.getUser().getId(), 0)
            ).reversed())
            .toList();
    }

    private List<LeaderboardEntryResponse> buildResponse(
        List<ContestParticipant> ranked,
        Map<Long, Integer> scoreByUserId
    ) {
        return java.util.stream.IntStream.range(0, ranked.size())
            .mapToObj(i -> {
                User user = ranked.get(i).getUser();
                return LeaderboardEntryResponse.builder()
                    .rank(i + 1)
                    .username(user.getUsername())
                    .score(scoreByUserId.getOrDefault(user.getId(), 0))
                    .build();
            })
            .toList();
    }

    private void applyRatingBonuses(List<ContestParticipant> ranked) {
        for (int i = 0; i < ranked.size(); i++) {
            ContestParticipant participant = ranked.get(i);
            if (participant.isRatingApplied()) {
                continue;
            }

            User user = participant.getUser();
            int bonus = PARTICIPATION_RATING_BONUS + (i < TOP_THREE_CUTOFF ? TOP_THREE_RATING_BONUS : 0);
            user.setRating(user.getRating() + bonus);
            userRepository.save(user);

            participant.setRatingApplied(true);
            contestParticipantRepository.save(participant);
        }
    }

}
