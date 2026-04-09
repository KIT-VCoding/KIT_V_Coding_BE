package com.example.thinkmap.dto;

import com.example.thinkmap.domain.entity.NodeType;
import com.example.thinkmap.domain.entity.ThoughtNode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 프론트엔드 마인드맵 렌더링용 계층형(Tree) 응답 DTO.
 * children 필드에 자식 노드가 재귀적으로 포함된다.
 */
@Schema(description = "마인드맵 트리 노드 (계층형)")
public record ThoughtTreeNode(
        @Schema(description = "노드 고유 ID") Long id,
        @Schema(description = "부모 노드 ID (루트 노드는 null)") Long parentNodeId,
        @Schema(description = "노드 타입") NodeType nodeType,
        @Schema(description = "노드 타입 한글 레이블 (UI 표시용)", example = "질문") String nodeTypeLabel,
        @Schema(description = "노드 전체 내용") String content,
        @Schema(description = "마인드맵 노드 라벨용 요약 (최대 80자)", nullable = true) String summary,
        @Schema(description = "트리 깊이 (루트 = 0)") int depth,
        @Schema(description = "노드 생성 일시") LocalDateTime createdAt,
        @Schema(description = "자식 노드 목록 (재귀)") List<ThoughtTreeNode> children
) {
    public static ThoughtTreeNode from(ThoughtNode node, List<ThoughtTreeNode> children, int depth) {
        return new ThoughtTreeNode(
                node.getId(),
                node.getParentNode() != null ? node.getParentNode().getId() : null,
                node.getNodeType(),
                node.getNodeType().label,
                node.getContent(),
                node.getSummary(),
                depth,
                node.getCreatedAt(),
                children
        );
    }
}
