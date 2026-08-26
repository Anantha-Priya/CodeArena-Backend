package com.codearena.service;

import com.codearena.dto.SubmissionRequest;
import com.codearena.dto.SubmissionResponse;
import com.codearena.entity.Contest;
import com.codearena.entity.Difficulty;
import com.codearena.entity.Problem;
import com.codearena.entity.Submission;
import com.codearena.entity.SubmissionStatus;
import com.codearena.entity.User;
import com.codearena.exception.BadRequestException;
import com.codearena.exception.ResourceNotFoundException;
import com.codearena.repository.ContestParticipantRepository;
import com.codearena.repository.ContestProblemRepository;
import com.codearena.repository.ContestRepository;
import com.codearena.repository.ProblemRepository;
import com.codearena.repository.SubmissionRepository;
import com.codearena.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private ContestRepository contestRepository;
    @Mock
    private ProblemRepository problemRepository;
    @Mock
    private ContestParticipantRepository contestParticipantRepository;
    @Mock
    private ContestProblemRepository contestProblemRepository;
    @Mock
    private UserRepository userRepository;

    private SubmissionService service;

    private User user;
    private Contest activeContest;
    private Problem mediumProblem;
    private SubmissionRequest request;

    @BeforeEach
    void setUp() {
        service = new SubmissionService(
            submissionRepository, contestRepository, problemRepository,
            contestParticipantRepository, contestProblemRepository, userRepository,
            new ScoreService()
        );

        user = User.builder().id(1L).email("user@codearena.com").build();
        activeContest = Contest.builder()
            .id(10L)
            .startTime(LocalDateTime.now().minusMinutes(1))
            .endTime(LocalDateTime.now().plusHours(1))
            .build();
        mediumProblem = Problem.builder().id(20L).difficulty(Difficulty.MEDIUM).build();
        request = new SubmissionRequest(10L, 20L, "java", "code", SubmissionStatus.ACCEPTED);

        when(userRepository.findByEmail("user@codearena.com")).thenReturn(Optional.of(user));
    }

    @Test
    void contestNotFoundThrows404() {
        when(contestRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(request, "user@codearena.com"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void problemNotFoundThrows404() {
        when(contestRepository.findById(10L)).thenReturn(Optional.of(activeContest));
        when(problemRepository.findById(20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(request, "user@codearena.com"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void notJoinedIsRejected() {
        when(contestRepository.findById(10L)).thenReturn(Optional.of(activeContest));
        when(problemRepository.findById(20L)).thenReturn(Optional.of(mediumProblem));
        when(contestParticipantRepository.existsByUserIdAndContestId(1L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> service.submit(request, "user@codearena.com"))
            .isInstanceOf(BadRequestException.class);

        verify(submissionRepository, never()).save(any());
    }

    @Test
    void contestNotActiveIsRejected() {
        Contest upcoming = Contest.builder()
            .id(10L)
            .startTime(LocalDateTime.now().plusHours(1))
            .endTime(LocalDateTime.now().plusHours(2))
            .build();
        when(contestRepository.findById(10L)).thenReturn(Optional.of(upcoming));
        when(problemRepository.findById(20L)).thenReturn(Optional.of(mediumProblem));
        when(contestParticipantRepository.existsByUserIdAndContestId(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> service.submit(request, "user@codearena.com"))
            .isInstanceOf(BadRequestException.class);

        verify(submissionRepository, never()).save(any());
    }

    @Test
    void problemNotInContestIsRejected() {
        when(contestRepository.findById(10L)).thenReturn(Optional.of(activeContest));
        when(problemRepository.findById(20L)).thenReturn(Optional.of(mediumProblem));
        when(contestParticipantRepository.existsByUserIdAndContestId(1L, 10L)).thenReturn(true);
        when(contestProblemRepository.existsByContestIdAndProblemId(10L, 20L)).thenReturn(false);

        assertThatThrownBy(() -> service.submit(request, "user@codearena.com"))
            .isInstanceOf(BadRequestException.class);

        verify(submissionRepository, never()).save(any());
    }

    @Test
    void acceptedMediumSubmissionScores200() {
        when(contestRepository.findById(10L)).thenReturn(Optional.of(activeContest));
        when(problemRepository.findById(20L)).thenReturn(Optional.of(mediumProblem));
        when(contestParticipantRepository.existsByUserIdAndContestId(1L, 10L)).thenReturn(true);
        when(contestProblemRepository.existsByContestIdAndProblemId(10L, 20L)).thenReturn(true);
        when(submissionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SubmissionResponse response = service.submit(request, "user@codearena.com");

        assertThat(response.getScore()).isEqualTo(200);

        ArgumentCaptor<Submission> captor = ArgumentCaptor.forClass(Submission.class);
        verify(submissionRepository).save(captor.capture());
        assertThat(captor.getValue().getScore()).isEqualTo(200);
    }

    @Test
    void wrongAnswerMediumSubmissionScoresZero() {
        SubmissionRequest wrongAnswerRequest =
            new SubmissionRequest(10L, 20L, "java", "code", SubmissionStatus.WRONG_ANSWER);
        when(contestRepository.findById(10L)).thenReturn(Optional.of(activeContest));
        when(problemRepository.findById(20L)).thenReturn(Optional.of(mediumProblem));
        when(contestParticipantRepository.existsByUserIdAndContestId(1L, 10L)).thenReturn(true);
        when(contestProblemRepository.existsByContestIdAndProblemId(10L, 20L)).thenReturn(true);
        when(submissionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SubmissionResponse response = service.submit(wrongAnswerRequest, "user@codearena.com");

        assertThat(response.getScore()).isZero();
    }

}
