package com.example.thinkmap.service;

import com.example.thinkmap.client.GeminiClient;
import com.example.thinkmap.domain.entity.LearningSession;
import com.example.thinkmap.domain.entity.NodeType;
import com.example.thinkmap.domain.entity.ThoughtNode;
import com.example.thinkmap.domain.repository.LearningSessionRepository;
import com.example.thinkmap.domain.repository.ThoughtNodeRepository;
import com.example.thinkmap.dto.PortfolioReportRequest;
import com.example.thinkmap.dto.PortfolioReportResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 학습 포트폴리오 보고서(MVP).
 * <p>
 * 계층: {@code PortfolioReportController} → 이 서비스 → {@link LearningSessionRepository} /
 * {@link ThoughtNodeRepository} + {@link GeminiClient}.
 * 세션에 저장된 노드가 곧 "Gemini 학습 로그"의 원천이며, 필요 시 요청의 {@code analysisJson}으로 결과를 덮어쓸 수 있다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioReportService {

    private final LearningSessionRepository sessionRepository;
    private final ThoughtNodeRepository nodeRepository;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PortfolioReportResponse generateReport(long sessionId, PortfolioReportRequest request, Long userId) {
        PortfolioReportRequest body = request != null ? request : PortfolioReportRequest.empty();

        LearningSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("세션을 찾을 수 없습니다. id=" + sessionId));
        if (!session.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("해당 세션에 접근 권한이 없습니다.");
        }

        if (body.hasAnalysisOverride()) {
            return objectMapper.convertValue(body.analysisJson(), PortfolioReportResponse.class);
        }

        List<ThoughtNode> nodes = nodeRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        long questionCount = nodes.stream()
                .filter(n -> n.getNodeType() == NodeType.QUESTION || n.getNodeType() == NodeType.REVISION)
                .count();
        if (questionCount == 0) {
            throw new IllegalStateException("포트폴리오를 만들려면 최소 1개의 질문(또는 이해 수정) 노드가 필요합니다.");
        }

        String prompt = buildPortfolioPrompt(session.getTitle(), nodes);
        String raw = geminiClient.generateStructuredRaw(prompt);
        String json = geminiClient.extractJsonObject(raw);
        if (json == null || json.isBlank()) {
            throw new IllegalStateException("AI 응답에서 JSON을 찾지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }
        try {
            return objectMapper.readValue(json, PortfolioReportResponse.class);
        } catch (Exception e) {
            log.warn("[PortfolioReportService] JSON 역직렬화 실패: {}", e.getMessage());
            throw new IllegalStateException("포트폴리오 JSON 형식이 올바르지 않습니다. 잠시 후 다시 시도해 주세요.", e);
        }
    }

    private String buildPortfolioPrompt(String sessionTitle, List<ThoughtNode> nodes) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                당신은 학습 데이터를 바탕으로 학생(또는 학습자)의 AI 학습 포트폴리오 보고서를 작성하는 교육 분석가입니다.
                아래는 한 학습 세션에서 기록된 노드 타임라인입니다. 내용만 근거로 추론하고, 없는 사실은 지어내지 마세요.

                """);

        sb.append("=== 세션 제목(학습 주제 후보) ===\n").append(sessionTitle).append("\n\n");
        sb.append("=== 노드 로그 (시간순) ===\n");
        for (ThoughtNode node : nodes) {
            switch (node.getNodeType()) {
                case QUESTION -> sb.append("[질문] ").append(truncate(node.getContent(), 2000));
                case AI_ANSWER -> sb.append("[AI 답변] ").append(truncate(node.getContent(), 2500));
                case REVISION -> sb.append("[이해 수정] ").append(truncate(node.getContent(), 2000));
                case INSIGHT -> sb.append("[기존 회고/인사이트] ").append(truncate(node.getContent(), 3000));
            }
            sb.append("\n\n");
        }

        sb.append("""
                ==============================

                위 로그만을 바탕으로 아래 JSON 키를 **정확히** 사용하고, 값은 모두 한국어로 채워라.
                코드블록·설명 문장 없이 **순수 JSON 한 덩어리**만 출력하라.

                {
                  "reportTitle": "보고서 제목 (한 줄, 임팩트 있게)",
                  "learningTopic": "이번 세션의 핵심 학습 주제 한 문장",
                  "initialQuestion": "첫 질문 또는 탐구 출발점 요약",
                  "coreExplorationProcess": "핵심 탐색 과정 (2~5문단, 마크다운 허용)",
                  "understandingTurningPoint": "이해가 크게 바뀌거나 깊어진 순간",
                  "revisedUnderstanding": "수정·정교화된 이해를 요약",
                  "finalSummary": "전체 학습을 관통하는 최종 정리",
                  "unresolvedConcepts": ["아직 애매하거나 더 파야 할 개념1", "개념2"],
                  "nextLearningQuestions": ["다음에 던지면 좋은 질문1", "질문2", "질문3"],
                  "oneLineReflection": "한 줄 회고 (60자 이내 권장)"
                }

                unresolvedConcepts·nextLearningQuestions는 각각 문자열 배열이며, 없으면 빈 배열 [] 로 둔다.
                """);
        return sb.toString();
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "…";
    }
}
