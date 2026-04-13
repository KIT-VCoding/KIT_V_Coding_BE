package com.example.thinkmap.domain.repository;

import com.example.thinkmap.domain.entity.LearningSession;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LearningSessionRepository extends JpaRepository<LearningSession, Long> {

    /**
     * 특정 사용자의 세션 목록을 최신 활동 순으로 조회.
     */
    @QueryHints(value = @QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    @Query("SELECT DISTINCT s FROM LearningSession s LEFT JOIN FETCH s.nodes " +
           "WHERE s.user.id = :userId " +
           "ORDER BY COALESCE(s.updatedAt, s.createdAt) DESC")
    List<LearningSession> findAllByUserIdWithNodesOrderByUpdatedAtDesc(@Param("userId") Long userId);

    /**
     * 단일 세션 + nodes fetch (상세 조회·SessionResponse 생성용).
     */
    @QueryHints(value = @QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    @Query("SELECT s FROM LearningSession s LEFT JOIN FETCH s.nodes WHERE s.id = :id")
    Optional<LearningSession> findByIdWithNodes(@Param("id") Long id);
}
