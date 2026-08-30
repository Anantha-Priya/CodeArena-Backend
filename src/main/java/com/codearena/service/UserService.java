package com.codearena.service;

import com.codearena.dto.UserProfileResponse;
import com.codearena.entity.SubmissionStatus;
import com.codearena.entity.User;
import com.codearena.exception.ResourceNotFoundException;
import com.codearena.repository.ContestParticipantRepository;
import com.codearena.repository.SubmissionRepository;
import com.codearena.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final ContestParticipantRepository contestParticipantRepository;

    public UserProfileResponse getMyProfile(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        return buildProfile(user);
    }

    /**
     * Admin-only (enforced in SecurityConfig, not here). Same per-user computation as
     * getMyProfile, just run once per row instead of once for the caller - O(1 + 2N) queries
     * for N users (one findAll, then one submissions lookup and one participant count per
     * user). Fine at this project's scale; would need a batched/aggregate query instead of
     * per-user lookups if the user base ever got large enough for that to matter.
     */
    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAll().stream()
            .map(this::buildProfile)
            .toList();
    }

    private UserProfileResponse buildProfile(User user) {
        long problemsSolved = submissionRepository.findByUserId(user.getId()).stream()
            .filter(submission -> submission.getStatus() == SubmissionStatus.ACCEPTED)
            .map(submission -> submission.getProblem().getId())
            .distinct()
            .count();

        long contestsJoined = contestParticipantRepository.countByUserId(user.getId());

        return UserProfileResponse.builder()
            .username(user.getUsername())
            .role(user.getRole())
            .rating(user.getRating())
            .problemsSolved(problemsSolved)
            .contestsJoined(contestsJoined)
            .build();
    }

}
