package com.codearena.service;

import com.codearena.dto.ProblemResponse;
import com.codearena.entity.Contest;
import com.codearena.entity.ContestProblem;
import com.codearena.entity.Problem;
import com.codearena.entity.Submission;
import com.codearena.exception.ResourceNotFoundException;
import com.codearena.repository.ContestParticipantRepository;
import com.codearena.repository.ContestProblemRepository;
import com.codearena.repository.ContestRepository;
import com.codearena.repository.ProblemRepository;
import com.codearena.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContestServiceTest {

    @Mock
    private ContestRepository contestRepository;

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private ContestProblemRepository contestProblemRepository;

    @Mock
    private ContestParticipantRepository contestParticipantRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private ProblemService problemService;

    private ContestService service;

    private Contest contest;

    @BeforeEach
    void setUp() {
        service = new ContestService(
            contestRepository, problemRepository, contestProblemRepository,
            contestParticipantRepository, submissionRepository, problemService
        );
        contest = Contest.builder().id(1L).title("Weekly Contest").build();
    }

    @Test
    void getProblemsForNonexistentContestThrowsNotFound() {
        when(contestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProblemsForContest(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getProblemsForContestWithNoneAttachedReturnsEmptyList() {
        when(contestRepository.findById(1L)).thenReturn(Optional.of(contest));
        when(contestProblemRepository.findByContestId(1L)).thenReturn(List.of());

        List<ProblemResponse> result = service.getProblemsForContest(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void getProblemsForContestReturnsEachAttachedProblemMapped() {
        Problem problemA = Problem.builder().id(10L).title("Two Sum").build();
        Problem problemB = Problem.builder().id(20L).title("Reverse String").build();
        ContestProblem linkA = ContestProblem.builder().contest(contest).problem(problemA).build();
        ContestProblem linkB = ContestProblem.builder().contest(contest).problem(problemB).build();

        ProblemResponse responseA = ProblemResponse.builder().id(10L).title("Two Sum").build();
        ProblemResponse responseB = ProblemResponse.builder().id(20L).title("Reverse String").build();

        when(contestRepository.findById(1L)).thenReturn(Optional.of(contest));
        when(contestProblemRepository.findByContestId(1L)).thenReturn(List.of(linkA, linkB));
        when(problemService.toResponse(problemA)).thenReturn(responseA);
        when(problemService.toResponse(problemB)).thenReturn(responseB);

        List<ProblemResponse> result = service.getProblemsForContest(1L);

        assertThat(result).containsExactly(responseA, responseB);
    }

    @Test
    void deleteNonexistentContestThrowsNotFound() {
        when(contestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
            .isInstanceOf(ResourceNotFoundException.class);

        verify(contestRepository, never()).delete(any());
    }

    @Test
    void deleteClearsSubmissionsRemovesJoinRowsThenDeletesTheContest() {
        Submission submission = Submission.builder().id(5L).contest(contest).build();

        when(contestRepository.findById(1L)).thenReturn(Optional.of(contest));
        when(submissionRepository.findByContestId(1L)).thenReturn(List.of(submission));

        service.delete(1L);

        // Submissions are reassigned to practice (contest = null), not deleted.
        assertThat(submission.getContest()).isNull();
        verify(submissionRepository).saveAll(List.of(submission));

        // The NOT NULL join rows must be gone before the contest row itself is deleted.
        verify(contestProblemRepository).deleteByContestId(1L);
        verify(contestParticipantRepository).deleteByContestId(1L);
        verify(contestRepository).delete(contest);
    }

    @Test
    void detachFromNonexistentContestThrowsNotFound() {
        when(contestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.detachProblemFromContest(99L, 10L))
            .isInstanceOf(ResourceNotFoundException.class);

        verify(contestProblemRepository, never()).deleteByContestIdAndProblemId(any(), any());
    }

    @Test
    void detachNonexistentProblemThrowsNotFound() {
        when(contestRepository.findById(1L)).thenReturn(Optional.of(contest));
        when(problemRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.detachProblemFromContest(1L, 99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void detachProblemNotAttachedThrowsNotFound() {
        when(contestRepository.findById(1L)).thenReturn(Optional.of(contest));
        when(problemRepository.existsById(10L)).thenReturn(true);
        when(contestProblemRepository.existsByContestIdAndProblemId(1L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> service.detachProblemFromContest(1L, 10L))
            .isInstanceOf(ResourceNotFoundException.class);

        verify(contestProblemRepository, never()).deleteByContestIdAndProblemId(any(), any());
    }

    @Test
    void detachAttachedProblemRemovesTheAssociation() {
        when(contestRepository.findById(1L)).thenReturn(Optional.of(contest));
        when(problemRepository.existsById(10L)).thenReturn(true);
        when(contestProblemRepository.existsByContestIdAndProblemId(1L, 10L)).thenReturn(true);

        service.detachProblemFromContest(1L, 10L);

        verify(contestProblemRepository).deleteByContestIdAndProblemId(1L, 10L);
    }

}
