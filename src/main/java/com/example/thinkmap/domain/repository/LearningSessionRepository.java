package com.example.thinkmap.domain.repository;

import com.example.thinkmap.domain.entity.LearningSession;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.List;
import java.util.Optional;

public interface LearningSessionRepository extends JpaRepository<LearningSession, Long> {

    /**
     * 세션 목록을 최신 활동 순(updatedAt이 없으면 createdAt 기준)으로 조회.
     * LEFT JOIN FETCH로 nodes를 함께 로딩(N+1 방지).
     * hibernate.query.passDistinctThrough=false : SQL에 DISTINCT를 전달하지 않고
     * Hibernate 레벨에서만 중복 제거 → ORDER BY 컬럼 제약 없이 안전하게 동작.
     */
    @QueryHints(value = @QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    @Query("SELECT DISTINCT s FROM LearningSession s LEFT JOIN FETCH s.nodes " +
           "ORDER BY COALESCE(s.updatedAt, s.createdAt) DESC")
    List<LearningSession> findAllWithNodesOrderByUpdatedAtDesc();

    /**
     * 단일 세션 + nodes fetch (상세 조회·SessionResponse 생성용).
     */
    @QueryHints(value = @QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    @Query("SELECT s FROM LearningSession s LEFT JOIN FETCH s.nodes WHERE s.id = :id")
    Optional<LearningSession> findByIdWithNodes(Long id);
}
