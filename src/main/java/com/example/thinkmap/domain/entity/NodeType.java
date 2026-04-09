package com.example.thinkmap.domain.entity;

/**
 * 학습 노드 타입.
 * label 필드는 프론트엔드 UI 표시용 한글 레이블이다.
 */
public enum NodeType {
    QUESTION("질문"),
    AI_ANSWER("AI 답변"),
    REVISION("이해 수정"),
    INSIGHT("학습 회고");

    public final String label;

    NodeType(String label) {
        this.label = label;
    }
}
