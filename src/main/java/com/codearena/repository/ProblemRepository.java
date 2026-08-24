package com.codearena.repository;

import com.codearena.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * JpaSpecificationExecutor chosen over separate findByDifficulty/findByTopic derived queries
 * (per the guide's own "or JpaSpecificationExecutor" option) — Phase 8 needs difficulty, topic,
 * and pagination combined as independently-optional filters, which a Specification handles
 * cleanly without a combinatorial explosion of derived method names.
 */
public interface ProblemRepository extends JpaRepository<Problem, Long>, JpaSpecificationExecutor<Problem> {
}
