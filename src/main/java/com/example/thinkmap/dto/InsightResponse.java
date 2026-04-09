package com.example.thinkmap.dto;

/**
 * POST /api/sessions/{id}/nodes/insight 응답 DTO.
 * AI가 생성한 학습 회고 인사이트 노드를 담는다.
 */
public record InsightResponse(
        ThoughtNodeResponse insightNode
) {}
