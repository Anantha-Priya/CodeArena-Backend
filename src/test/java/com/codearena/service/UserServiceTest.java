package com.codearena.service;

import com.codearena.dto.UserProfileResponse;
import com.codearena.entity.Problem;
import com.codearena.entity.Submission;
import com.codearena.entity.SubmissionStatus;
import com.codearena.entity.User;
import com.codearena.repository.ContestParticipantRepository;
import com.codearena.repository.SubmissionRepository;
import com.codearena.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private ContestParticipantRepository contestParticipantRepository;

    @Test
    void problemsSolvedCountsDistinctAcceptedProblemsNotTotalSubmissions() {
        UserService service = new UserService(userRepository, submissionRepository, contestParticipantRepository);
        User user = User.builder().id(1L).username("alice").rating(70).build();
        Problem problemA = Problem.builder().id(10L).build();
        Problem problemB = Problem.builder().id(20L).build();

        when(userRepository.findByEmail("alice@codearena.com")).thenReturn(Optional.of(user));
        when(submissionRepository.findByUserId(1L)).thenReturn(List.of(
            Submission.builder().problem(problemA).status(SubmissionStatus.ACCEPTED).build(),
            Submission.builder().problem(problemA).status(SubmissionStatus.ACCEPTED).build(), // same problem again
            Submission.builder().problem(problemB).status(SubmissionStatus.WRONG_ANSWER).build()
        ));
        when(contestParticipantRepository.countByUserId(1L)).thenReturn(3L);

        UserProfileResponse response = service.getMyProfile("alice@codearena.com");

        assertThat(response.getUsername()).isEqualTo("alice");
        assertThat(response.getRating()).isEqualTo(70);
        assertThat(response.getProblemsSolved()).isEqualTo(1);
        assertThat(response.getContestsJoined()).isEqualTo(3);
    }

}
