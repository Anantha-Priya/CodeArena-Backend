package com.codearena.repository;

import com.codearena.entity.ContestParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContestParticipantRepository extends JpaRepository<ContestParticipant, Long> {

    boolean existsByUserIdAndContestId(Long userId, Long contestId);

    List<ContestParticipant> findByContestId(Long contestId);

}
