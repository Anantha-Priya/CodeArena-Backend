package com.codearena.repository;

import com.codearena.entity.ContestProblem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContestProblemRepository extends JpaRepository<ContestProblem, Long> {

    boolean existsByContestIdAndProblemId(Long contestId, Long problemId);

    List<ContestProblem> findByContestId(Long contestId);

}
