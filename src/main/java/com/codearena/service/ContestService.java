package com.codearena.service;

import com.codearena.dto.ContestRequest;
import com.codearena.dto.ContestResponse;
import com.codearena.dto.ContestStatusResponse;
import com.codearena.dto.ProblemResponse;
import com.codearena.entity.Contest;
import com.codearena.entity.ContestProblem;
import com.codearena.entity.ContestStatus;
import com.codearena.entity.Problem;
import com.codearena.entity.Submission;
import com.codearena.exception.DuplicateResourceException;
import com.codearena.exception.ResourceNotFoundException;
import com.codearena.repository.ContestParticipantRepository;
import com.codearena.repository.ContestProblemRepository;
import com.codearena.repository.ContestRepository;
import com.codearena.repository.ProblemRepository;
import com.codearena.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContestService {

    private final ContestRepository contestRepository;
    private final ProblemRepository problemRepository;
    private final ContestProblemRepository contestProblemRepository;
    private final ContestParticipantRepository contestParticipantRepository;
    private final SubmissionRepository submissionRepository;
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

    /**
     * Three tables reference contests, none cascading: contest_problems and
     * contest_participants both have a NOT NULL contest_id, so those rows must be removed
     * before the contest row itself, or the delete violates their FK constraint. submissions
     * has a nullable contest_id (a practice submission already has no contest) - those rows
     * are kept and their contest reference cleared instead of deleted, so a user doesn't lose
     * submission history just because an admin removed the contest it was made in. All of it
     * runs in one transaction: either the whole cleanup commits, or none of it does.
     */
    @Transactional
    public void delete(Long id) {
        Contest contest = findByIdOrThrow(id);

        List<Submission> submissions = submissionRepository.findByContestId(id);
        submissions.forEach(submission -> submission.setContest(null));
        submissionRepository.saveAll(submissions);

        contestProblemRepository.deleteByContestId(id);
        contestParticipantRepository.deleteByContestId(id);

        contestRepository.delete(contest);
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

    /**
     * Mirrors addProblemToContest's checks in reverse: contest exists, problem exists, then
     * the association itself exists - 404 if the problem was never attached to this contest,
     * matching how the inverse (attaching twice) is a 409, not a silent no-op. Submissions
     * already made against this problem/contest pair are untouched - they FK to the contest
     * and problem directly, not to this join row, so detaching can't orphan them.
     */
    public void detachProblemFromContest(Long contestId, Long problemId) {
        findByIdOrThrow(contestId);
        if (!problemRepository.existsById(problemId)) {
            throw new ResourceNotFoundException("Problem not found: " + problemId);
        }
        if (!contestProblemRepository.existsByContestIdAndProblemId(contestId, problemId)) {
            throw new ResourceNotFoundException("Problem " + problemId + " is not attached to contest " + contestId);
        }

        contestProblemRepository.deleteByContestIdAndProblemId(contestId, problemId);
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
        Instant now = Instant.now();

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
