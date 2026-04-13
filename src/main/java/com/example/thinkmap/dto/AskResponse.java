package com.example.thinkmap.dto;

/**
 * 질문 추가 API 응답: 저장된 질문 노드 + AI 답변 노드를 함께 반환.
 * aiError가 true이면 Gemini 호출 실패 — answerNode는 null.
 */
public record AskResponse(
        ThoughtNodeResponse questionNode,
        ThoughtNodeResponse answerNode,
        boolean aiError
) {
    /** AI 성공 응답 */
    public static AskResponse of(ThoughtNodeResponse questionNode, ThoughtNodeResponse answerNode) {
        return new AskResponse(questionNode, answerNode, false);
    }

    /** AI 실패 응답 — answerNode 없음 */
    public static AskResponse ofAiError(ThoughtNodeResponse questionNode) {
        return new AskResponse(questionNode, null, true);
    }
}
