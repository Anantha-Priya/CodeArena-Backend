package com.codearena.service;

import com.codearena.dto.ProblemRequest;
import com.codearena.dto.ProblemResponse;
import com.codearena.entity.Difficulty;
import com.codearena.entity.Problem;
import com.codearena.exception.ResourceNotFoundException;
import com.codearena.repository.ProblemRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemRepository problemRepository;

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

    public void delete(Long id) {
        Problem problem = findByIdOrThrow(id);
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
