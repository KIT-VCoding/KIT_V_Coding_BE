package com.example.thinkmap.dto;

import com.example.thinkmap.domain.entity.NodeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "질문 또는 이해 수정 노드 추가 요청")
public record AddQuestionRequest(
        @Schema(description = "질문 또는 이해 수정 내용", example = "광합성이란 무엇인가요?")
        @NotBlank(message = "질문 내용은 필수입니다.")
        @Size(max = 2000, message = "질문 내용은 2000자를 초과할 수 없습니다.")
        String content,

        @Schema(description = "부모 노드 ID. null이면 루트 질문, 값이 있으면 해당 노드 아래 심화 질문.", nullable = true)
        Long parentNodeId,

        @Schema(description = "노드 타입. QUESTION(기본값) 또는 REVISION(이해 수정·심화 탐색). null이면 QUESTION으로 처리.",
                allowableValues = {"QUESTION", "REVISION"}, nullable = true)
        NodeType nodeType
) {
    /** 입력값 정규화 (앞뒤 공백 제거) */
    public AddQuestionRequest {
        if (content != null) content = content.strip();
    }

    /** QUESTION 또는 REVISION만 허용. null이면 QUESTION 반환. */
    public NodeType resolvedNodeType() {
        return nodeType == NodeType.REVISION ? NodeType.REVISION : NodeType.QUESTION;
    }

    /**
     * AI_ANSWER·INSIGHT는 클라이언트가 직접 지정 불가.
     * 서비스 진입 전에 호출한다.
     */
    public void validateNodeType() {
        if (nodeType == NodeType.AI_ANSWER || nodeType == NodeType.INSIGHT) {
            throw new IllegalArgumentException(
                    "nodeType은 QUESTION 또는 REVISION만 허용됩니다. 요청값: " + nodeType);
        }
    }
}
