package com.codearena.repository;

import com.codearena.entity.Submission;
import com.codearena.entity.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByUserId(Long userId);

    List<Submission> findByContestIdAndStatus(Long contestId, SubmissionStatus status);

    List<Submission> findByUserIdOrderBySubmittedAtDesc(Long userId);

}
