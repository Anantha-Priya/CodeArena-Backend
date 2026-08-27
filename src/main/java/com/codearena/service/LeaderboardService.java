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

        List<ContestParticipant> participants = contestParticipantRepository.findByContestId(contestId);

        Map<Long, Integer> scoreByUserId = submissionRepository
            .findByContestIdAndStatus(contestId, SubmissionStatus.ACCEPTED).stream()
            .collect(Collectors.groupingBy(
                submission -> submission.getUser().getId(),
                Collectors.summingInt(Submission::getScore)
            ));

        List<ContestParticipant> ranked = participants.stream()
            .sorted(Comparator.comparingInt(
                (ContestParticipant p) -> scoreByUserId.getOrDefault(p.getUser().getId(), 0)
            ).reversed())
            .toList();

        if (contest.getStatus() == ContestStatus.ENDED) {
            applyRatingBonuses(ranked);
        }

        return buildResponse(ranked, scoreByUserId);
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
