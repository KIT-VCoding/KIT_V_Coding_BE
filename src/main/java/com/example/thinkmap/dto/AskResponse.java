package com.example.thinkmap.dto;

/**
 * 질문 추가 API 응답: 저장된 질문 노드 + AI 답변 노드를 함께 반환
 */
public record AskResponse(
        ThoughtNodeResponse questionNode,
        ThoughtNodeResponse answerNode
) {}
