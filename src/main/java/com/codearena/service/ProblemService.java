package com.codearena.service;

import com.codearena.dto.ProblemRequest;
import com.codearena.dto.ProblemResponse;
import com.codearena.entity.Difficulty;
import com.codearena.entity.Problem;
import com.codearena.exception.ResourceInUseException;
import com.codearena.exception.ResourceNotFoundException;
import com.codearena.repository.ContestProblemRepository;
import com.codearena.repository.ProblemRepository;
import com.codearena.repository.SubmissionRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final ContestProblemRepository contestProblemRepository;
    private final SubmissionRepository submissionRepository;

    public ProblemResponse create(ProblemRequest request) {
        Problem problem = Problem.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .difficulty(request.getDifficulty())
            .topic(request.getTopic())
            .constraints(request.getConstraints())
            .inputFormat(request.getInputFormat())
            .outputFormat(request.getOutputFormat())
            .sampleInput(request.getSampleInput())
            .sampleOutput(request.getSampleOutput())
            .build();

        return toResponse(problemRepository.save(problem));
    }

    public ProblemResponse update(Long id, ProblemRequest request) {
        Problem problem = findByIdOrThrow(id);

        problem.setTitle(request.getTitle());
        problem.setDescription(request.getDescription());
        problem.setDifficulty(request.getDifficulty());
        problem.setTopic(request.getTopic());
        problem.setConstraints(request.getConstraints());
        problem.setInputFormat(request.getInputFormat());
        problem.setOutputFormat(request.getOutputFormat());
        problem.setSampleInput(request.getSampleInput());
        problem.setSampleOutput(request.getSampleOutput());

        return toResponse(problemRepository.save(problem));
    }

    /**
     * Two tables reference problems, both NOT NULL, neither cascading: contest_problems and
     * submissions. Unlike a contest's submissions.contest_id (nullable - "no contest" means
     * a practice submission), submissions.problem_id has no null-out escape hatch - a
     * submission can't exist without a problem, and cascade-deleting a user's submissions
     * because an admin removed a problem would be real data loss. So this blocks with a 409
     * instead: once a problem has been submitted against, it's part of someone's history and
     * isn't deletable. A problem with no submissions but still attached to a contest is just
     * an admin-managed association - safe to auto-detach (the admin could already do this one
     * contest at a time via the detach endpoint) and then delete.
     */
    @Transactional
    public void delete(Long id) {
        Problem problem = findByIdOrThrow(id);

        if (submissionRepository.existsByProblemId(id)) {
            throw new ResourceInUseException("Cannot delete problem " + id + ": submissions exist against it");
        }

        contestProblemRepository.deleteByProblemId(id);
        problemRepository.delete(problem);
    }

    public ProblemResponse getById(Long id) {
        return toResponse(findByIdOrThrow(id));
    }

    public Page<ProblemResponse> search(Difficulty difficulty, String topic, Pageable pageable) {
        return problemRepository.findAll(buildSpecification(difficulty, topic), pageable)
            .map(this::toResponse);
    }

    private Problem findByIdOrThrow(Long id) {
        return problemRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Problem not found: " + id));
    }

    private Specification<Problem> buildSpecification(Difficulty difficulty, String topic) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (difficulty != null) {
                predicates.add(cb.equal(root.get("difficulty"), difficulty));
            }
            if (topic != null && !topic.isBlank()) {
                predicates.add(cb.equal(root.get("topic"), topic));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // Package-private (not private) so ContestService can reuse this exact mapping for
    // GET /api/contests/{id}/problems instead of duplicating it.
    ProblemResponse toResponse(Problem problem) {
        return ProblemResponse.builder()
            .id(problem.getId())
            .title(problem.getTitle())
            .description(problem.getDescription())
            .difficulty(problem.getDifficulty())
            .topic(problem.getTopic())
            .constraints(problem.getConstraints())
            .inputFormat(problem.getInputFormat())
            .outputFormat(problem.getOutputFormat())
            .sampleInput(problem.getSampleInput())
            .sampleOutput(problem.getSampleOutput())
            .createdAt(problem.getCreatedAt())
            .build();
    }

}
