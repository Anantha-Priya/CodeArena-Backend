package com.codearena.service;

import com.codearena.entity.Contest;
import com.codearena.entity.ContestParticipant;
import com.codearena.entity.ContestStatus;
import com.codearena.entity.User;
import com.codearena.exception.BadRequestException;
import com.codearena.exception.DuplicateResourceException;
import com.codearena.exception.ResourceNotFoundException;
import com.codearena.repository.ContestParticipantRepository;
import com.codearena.repository.ContestRepository;
import com.codearena.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Authentication itself is already enforced by SecurityConfig for this path - the checks
 * here start from "contest exists", per the guide's own ordering: authenticated -> contest
 * exists -> contest has not ended -> user hasn't already joined.
 */
@Service
@RequiredArgsConstructor
public class ContestParticipantService {

    private final ContestRepository contestRepository;
    private final UserRepository userRepository;
    private final ContestParticipantRepository contestParticipantRepository;

    public void join(Long contestId, String userEmail) {
        Contest contest = contestRepository.findById(contestId)
            .orElseThrow(() -> new ResourceNotFoundException("Contest not found: " + contestId));

        if (contest.getStatus() == ContestStatus.ENDED) {
            throw new BadRequestException("Contest has already ended");
        }

        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        if (contestParticipantRepository.existsByUserIdAndContestId(user.getId(), contestId)) {
            throw new DuplicateResourceException("Already joined this contest");
        }

        contestParticipantRepository.save(ContestParticipant.builder()
            .user(user)
            .contest(contest)
            .build());
    }

}
