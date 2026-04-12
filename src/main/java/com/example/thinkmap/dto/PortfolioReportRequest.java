package com.example.thinkmap.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;
import java.util.Set;

/**
 * 포트폴리오 생성 요청 본문(선택).
 * {@code analysisJson}에 {@link PortfolioReportResponse}와 동일한 키(camelCase)가
 * <strong>하나라도</strong> 있을 때만 Gemini 호출 없이 그 JSON을 응답으로 변환한다.
 * Swagger 기본 placeholder({@code additionalProp*})만 있는 경우는 무시하고 일반 생성으로 처리한다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PortfolioReportRequest(
        Map<String, Object> analysisJson
) {
    private static final Set<String> OVERRIDE_KEYS = Set.of(
            "reportTitle",
            "learningTopic",
            "initialQuestion",
            "coreExplorationProcess",
            "understandingTurningPoint",
            "revisedUnderstanding",
            "finalSummary",
            "unresolvedConcepts",
            "nextLearningQuestions",
            "oneLineReflection"
    );

    public static PortfolioReportRequest empty() {
        return new PortfolioReportRequest(null);
    }

    public boolean hasAnalysisOverride() {
        if (analysisJson == null || analysisJson.isEmpty()) {
            return false;
        }
        return analysisJson.keySet().stream().anyMatch(OVERRIDE_KEYS::contains);
    }
}
