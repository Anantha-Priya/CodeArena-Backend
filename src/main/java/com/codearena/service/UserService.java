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

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final ContestParticipantRepository contestParticipantRepository;

    public UserProfileResponse getMyProfile(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        long problemsSolved = submissionRepository.findByUserId(user.getId()).stream()
            .filter(submission -> submission.getStatus() == SubmissionStatus.ACCEPTED)
            .map(submission -> submission.getProblem().getId())
            .distinct()
            .count();

        long contestsJoined = contestParticipantRepository.countByUserId(user.getId());

        return UserProfileResponse.builder()
            .username(user.getUsername())
            .rating(user.getRating())
            .problemsSolved(problemsSolved)
            .contestsJoined(contestsJoined)
            .build();
    }

}
