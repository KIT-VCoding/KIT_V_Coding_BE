package com.example.thinkmap.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * AI 학습 포트폴리오 보고서 — 프론트 보고서 UI에 그대로 바인딩 가능한 형태.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PortfolioReportResponse(
        String reportTitle,
        String learningTopic,
        String initialQuestion,
        String coreExplorationProcess,
        String understandingTurningPoint,
        String revisedUnderstanding,
        String finalSummary,
        List<String> unresolvedConcepts,
        List<String> nextLearningQuestions,
        String oneLineReflection
) {
    public PortfolioReportResponse {
        reportTitle = blankToEmpty(reportTitle);
        learningTopic = blankToEmpty(learningTopic);
        initialQuestion = blankToEmpty(initialQuestion);
        coreExplorationProcess = blankToEmpty(coreExplorationProcess);
        understandingTurningPoint = blankToEmpty(understandingTurningPoint);
        revisedUnderstanding = blankToEmpty(revisedUnderstanding);
        finalSummary = blankToEmpty(finalSummary);
        oneLineReflection = blankToEmpty(oneLineReflection);
        unresolvedConcepts = unresolvedConcepts != null ? List.copyOf(unresolvedConcepts) : List.of();
        nextLearningQuestions = nextLearningQuestions != null ? List.copyOf(nextLearningQuestions) : List.of();
    }

    private static String blankToEmpty(String s) {
        return s == null ? "" : s;
    }
}
