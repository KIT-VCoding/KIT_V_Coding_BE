package com.example.thinkmap.dto;

import com.example.thinkmap.domain.entity.LearningSession;
import com.example.thinkmap.domain.entity.NodeType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "학습 세션 응답")
public record SessionResponse(
        @Schema(description = "세션 고유 ID") Long id,
        @Schema(description = "소유자 사용자 ID") Long userId,
        @Schema(description = "세션 제목") String title,
        @Schema(description = "세션 생성 일시") LocalDateTime createdAt,
        @Schema(description = "마지막 활동 일시") LocalDateTime updatedAt,
        @Schema(description = "전체 노드 수 (질문·AI답변·이해수정·인사이트 합산)") int nodeCount,
        @Schema(description = "질문 노드 수 (QUESTION + REVISION)") int questionCount,
        @Schema(description = "학습 회고 인사이트 생성 여부") boolean hasInsight
) {
    public static SessionResponse from(LearningSession session) {
        int qCount = (int) session.getNodes().stream()
                .filter(n -> n.getNodeType() == NodeType.QUESTION || n.getNodeType() == NodeType.REVISION)
                .count();
        boolean insight = session.getNodes().stream()
                .anyMatch(n -> n.getNodeType() == NodeType.INSIGHT);
        return new SessionResponse(
                session.getId(),
                session.getUser().getId(),
                session.getTitle(),
                session.getCreatedAt(),
                session.getUpdatedAt(),
                session.getNodes().size(),
                qCount,
                insight
        );
    }
}
