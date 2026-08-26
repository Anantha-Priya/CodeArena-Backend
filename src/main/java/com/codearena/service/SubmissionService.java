package com.codearena.service;

import com.codearena.dto.SubmissionRequest;
import com.codearena.dto.SubmissionResponse;
import com.codearena.entity.Contest;
import com.codearena.entity.ContestStatus;
import com.codearena.entity.Problem;
import com.codearena.entity.Submission;
import com.codearena.entity.User;
import com.codearena.exception.BadRequestException;
import com.codearena.exception.ResourceNotFoundException;
import com.codearena.repository.ContestParticipantRepository;
import com.codearena.repository.ContestProblemRepository;
import com.codearena.repository.ContestRepository;
import com.codearena.repository.ProblemRepository;
import com.codearena.repository.SubmissionRepository;
import com.codearena.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Authentication itself is already enforced by SecurityConfig - the checks here follow the
 * guide's remaining order: contest exists -> problem exists -> user has joined -> contest is
 * ACTIVE -> problem belongs to the contest -> create.
 */
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ContestRepository contestRepository;
    private final ProblemRepository problemRepository;
    private final ContestParticipantRepository contestParticipantRepository;
    private final ContestProblemRepository contestProblemRepository;
    private final UserRepository userRepository;
    private final ScoreService scoreService;

    public SubmissionResponse submit(SubmissionRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        Contest contest = contestRepository.findById(request.getContestId())
            .orElseThrow(() -> new ResourceNotFoundException("Contest not found: " + request.getContestId()));

        Problem problem = problemRepository.findById(request.getProblemId())
            .orElseThrow(() -> new ResourceNotFoundException("Problem not found: " + request.getProblemId()));

        if (!contestParticipantRepository.existsByUserIdAndContestId(user.getId(), contest.getId())) {
            throw new BadRequestException("You have not joined this contest");
        }

        if (contest.getStatus() != ContestStatus.ACTIVE) {
            throw new BadRequestException("Contest is not currently active");
        }

        if (!contestProblemRepository.existsByContestIdAndProblemId(contest.getId(), problem.getId())) {
            throw new BadRequestException("Problem does not belong to this contest");
        }

        int score = scoreService.calculateScore(request.getStatus(), problem.getDifficulty());

        Submission submission = Submission.builder()
            .user(user)
            .problem(problem)
            .contest(contest)
            .language(request.getLanguage())
            .sourceCode(request.getSourceCode())
            .status(request.getStatus())
            .score(score)
            .build();

        return toResponse(submissionRepository.save(submission));
    }

    public List<SubmissionResponse> getMySubmissions(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        return submissionRepository.findByUserIdOrderBySubmittedAtDesc(user.getId()).stream()
            .map(this::toResponse)
            .toList();
    }

    private SubmissionResponse toResponse(Submission submission) {
        return SubmissionResponse.builder()
            .id(submission.getId())
            .problemId(submission.getProblem().getId())
            .problemTitle(submission.getProblem().getTitle())
            .contestId(submission.getContest().getId())
            .contestTitle(submission.getContest().getTitle())
            .language(submission.getLanguage())
            .status(submission.getStatus())
            .score(submission.getScore())
            .submittedAt(submission.getSubmittedAt())
            .build();
    }

}
