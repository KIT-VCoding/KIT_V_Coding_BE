package com.example.thinkmap.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "thought_node", indexes = {
        @Index(name = "idx_thought_node_session_id", columnList = "session_id"),
        @Index(name = "idx_thought_node_parent_id", columnList = "parent_node_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ThoughtNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 소속 세션 (N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private LearningSession session;

    // 자기 참조 (Self-Join) — 최상위 노드는 null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_node_id")
    private ThoughtNode parentNode;

    // 자식 노드 목록 (트리 구성용)
    @OneToMany(mappedBy = "parentNode", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ThoughtNode> children = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "node_type", nullable = false, length = 20)
    private NodeType nodeType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 마인드맵 노드 라벨용 짧은 요약 (AI 답변/인사이트 노드에만 존재, 최대 80자)
    @Column(length = 80)
    private String summary;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ThoughtNode(LearningSession session, ThoughtNode parentNode, NodeType nodeType, String content, String summary) {
        this.session = session;
        this.parentNode = parentNode;
        this.nodeType = nodeType;
        this.content = content;
        this.summary = summary;
    }
}
