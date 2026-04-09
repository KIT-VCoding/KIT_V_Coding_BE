package com.example.thinkmap.dto;

import com.example.thinkmap.domain.entity.NodeType;
import com.example.thinkmap.domain.entity.ThoughtNode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "학습 노드 응답")
public record ThoughtNodeResponse(
        @Schema(description = "노드 고유 ID") Long id,
        @Schema(description = "소속 세션 ID") Long sessionId,
        @Schema(description = "부모 노드 ID (루트 노드는 null)") Long parentNodeId,
        @Schema(description = "노드 타입 (QUESTION·AI_ANSWER·REVISION·INSIGHT)") NodeType nodeType,
        @Schema(description = "노드 타입 한글 레이블 (UI 표시용)", example = "AI 답변") String nodeTypeLabel,
        @Schema(description = "노드 전체 내용") String content,
        @Schema(description = "마인드맵 노드 라벨용 요약 (AI_ANSWER·INSIGHT 노드에만 존재, 최대 80자)", nullable = true) String summary,
        @Schema(description = "노드 생성 일시") LocalDateTime createdAt
) {
    public static ThoughtNodeResponse from(ThoughtNode node) {
        return new ThoughtNodeResponse(
                node.getId(),
                node.getSession().getId(),
                node.getParentNode() != null ? node.getParentNode().getId() : null,
                node.getNodeType(),
                node.getNodeType().label,
                node.getContent(),
                node.getSummary(),
                node.getCreatedAt()
        );
    }
}
