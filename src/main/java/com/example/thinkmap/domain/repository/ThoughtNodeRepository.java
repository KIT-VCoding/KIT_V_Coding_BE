package com.example.thinkmap.domain.repository;

import com.example.thinkmap.domain.entity.ThoughtNode;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ThoughtNodeRepository extends JpaRepository<ThoughtNode, Long> {

    // 특정 세션의 모든 노드를 생성 순서대로 조회 (컨텍스트·인사이트 프롬프트 구성용)
    List<ThoughtNode> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    // 특정 세션의 루트 노드(parent == null)만 조회 (트리 탐색 시작점)
    @Query("SELECT n FROM ThoughtNode n WHERE n.session.id = :sessionId AND n.parentNode IS NULL ORDER BY n.createdAt ASC")
    List<ThoughtNode> findRootNodesBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 특정 세션의 모든 노드를 children까지 fetch join하여 한 번에 조회 (N+1 방지).
     * passDistinctThrough=false : SQL DISTINCT 없이 Hibernate 레벨 중복 제거.
     * → DISTINCT + LEFT JOIN FETCH + ORDER BY 의 DB 호환성 문제 방지.
     */
    @QueryHints(value = @QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    @Query("SELECT DISTINCT n FROM ThoughtNode n LEFT JOIN FETCH n.children " +
           "WHERE n.session.id = :sessionId ORDER BY n.createdAt ASC")
    List<ThoughtNode> findAllWithChildrenBySessionId(@Param("sessionId") Long sessionId);
}
