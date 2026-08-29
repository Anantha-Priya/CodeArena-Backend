package com.codearena.service;

import com.codearena.dto.ContestRequest;
import com.codearena.dto.ContestResponse;
import com.codearena.dto.ContestStatusResponse;
import com.codearena.dto.ProblemResponse;
import com.codearena.entity.Contest;
import com.codearena.entity.ContestProblem;
import com.codearena.entity.ContestStatus;
import com.codearena.entity.Problem;
import com.codearena.exception.DuplicateResourceException;
import com.codearena.exception.ResourceNotFoundException;
import com.codearena.repository.ContestProblemRepository;
import com.codearena.repository.ContestRepository;
import com.codearena.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContestService {

    private final ContestRepository contestRepository;
    private final ProblemRepository problemRepository;
    private final ContestProblemRepository contestProblemRepository;
    private final ProblemService problemService;

    public ContestResponse create(ContestRequest request) {
        Contest contest = Contest.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .startTime(request.getStartTime())
            .endTime(request.getEndTime())
            .build();

        return toResponse(contestRepository.save(contest));
    }

    public List<ContestResponse> getAll() {
        return contestRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public ContestResponse getById(Long id) {
        return toResponse(findByIdOrThrow(id));
    }

    public void addProblemToContest(Long contestId, Long problemId) {
        Contest contest = findByIdOrThrow(contestId);
        Problem problem = problemRepository.findById(problemId)
            .orElseThrow(() -> new ResourceNotFoundException("Problem not found: " + problemId));

        if (contestProblemRepository.existsByContestIdAndProblemId(contestId, problemId)) {
            throw new DuplicateResourceException("Problem already added to this contest");
        }

        contestProblemRepository.save(ContestProblem.builder()
            .contest(contest)
            .problem(problem)
            .build());
    }

    public List<ProblemResponse> getProblemsForContest(Long contestId) {
        findByIdOrThrow(contestId);

        return contestProblemRepository.findByContestId(contestId).stream()
            .map(ContestProblem::getProblem)
            .map(problemService::toResponse)
            .toList();
    }

    public ContestStatusResponse getStatus(Long id) {
        Contest contest = findByIdOrThrow(id);
        ContestStatus status = contest.getStatus();
        LocalDateTime now = LocalDateTime.now();

        long remainingSeconds = switch (status) {
            case UPCOMING -> Duration.between(now, contest.getStartTime()).getSeconds();
            case ACTIVE -> Duration.between(now, contest.getEndTime()).getSeconds();
            case ENDED -> 0;
        };

        return ContestStatusResponse.builder()
            .status(status.name())
            .remainingSeconds(remainingSeconds)
            .build();
    }

    private Contest findByIdOrThrow(Long id) {
        return contestRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Contest not found: " + id));
    }

    private ContestResponse toResponse(Contest contest) {
        return ContestResponse.builder()
            .id(contest.getId())
            .title(contest.getTitle())
            .description(contest.getDescription())
            .startTime(contest.getStartTime())
            .endTime(contest.getEndTime())
            .createdAt(contest.getCreatedAt())
            .build();
    }

}
