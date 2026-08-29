package com.codearena.service;

import com.codearena.dto.ProblemResponse;
import com.codearena.entity.Contest;
import com.codearena.entity.ContestProblem;
import com.codearena.entity.Problem;
import com.codearena.exception.ResourceNotFoundException;
import com.codearena.repository.ContestProblemRepository;
import com.codearena.repository.ContestRepository;
import com.codearena.repository.ProblemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private ProblemService problemService;

    private ContestService service;

    private Contest contest;

    @BeforeEach
    void setUp() {
        service = new ContestService(contestRepository, problemRepository, contestProblemRepository, problemService);
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

}
