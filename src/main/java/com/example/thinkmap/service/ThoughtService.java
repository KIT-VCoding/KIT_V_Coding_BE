package com.example.thinkmap.service;

import com.example.thinkmap.client.GeminiClient;
import com.example.thinkmap.domain.entity.LearningSession;
import com.example.thinkmap.domain.entity.NodeType;
import com.example.thinkmap.domain.entity.ThoughtNode;
import com.example.thinkmap.domain.repository.LearningSessionRepository;
import com.example.thinkmap.domain.repository.ThoughtNodeRepository;
import com.example.thinkmap.dto.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThoughtService {

    private final LearningSessionRepository sessionRepository;
    private final ThoughtNodeRepository nodeRepository;
    private final GeminiClient geminiClient;

    // ──────────────────────────────────────────────
    // 세션 생성
    // ──────────────────────────────────────────────

    @Transactional
    public SessionResponse createSession(CreateSessionRequest request) {
        LearningSession session = LearningSession.builder()
                .title(request.title())
                .build();
        LearningSession saved = sessionRepository.save(session);
        log.info("[ThoughtService] 세션 생성 완료 - id: {}, title: {}", saved.getId(), saved.getTitle());
        return SessionResponse.from(saved);
    }

    // ──────────────────────────────────────────────
    // 세션 목록 조회 (최신 활동 순)
    // ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SessionResponse> listSessions() {
        return sessionRepository.findAllWithNodesOrderByUpdatedAtDesc()
                .stream()
                .map(SessionResponse::from)
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────
    // 세션 상세 조회
    // ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public SessionResponse getSession(Long sessionId) {
        LearningSession session = sessionRepository.findByIdWithNodes(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("세션을 찾을 수 없습니다. id=" + sessionId));
        return SessionResponse.from(session);
    }

    // ──────────────────────────────────────────────
    // 세션 제목 수정
    // ──────────────────────────────────────────────

    @Transactional
    public SessionResponse updateSessionTitle(Long sessionId, UpdateSessionTitleRequest request) {
        LearningSession session = sessionRepository.findByIdWithNodes(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("세션을 찾을 수 없습니다. id=" + sessionId));
        session.updateTitle(request.title());
        // @UpdateTimestamp가 merge 시 updated_at을 자동 갱신
        sessionRepository.save(session);
        log.info("[ThoughtService] 세션 제목 수정 완료 - id: {}, newTitle: {}", sessionId, request.title());
        return SessionResponse.from(session);
    }

    // ──────────────────────────────────────────────
    // 세션 삭제
    // ──────────────────────────────────────────────

    @Transactional
    public void deleteSession(Long sessionId) {
        // findById로 존재 확인 후 삭제 (existsById + deleteById TOCTOU 패턴 제거)
        LearningSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("세션을 찾을 수 없습니다. id=" + sessionId));
        sessionRepository.delete(session);  // CascadeType.ALL로 연관 노드도 삭제
        log.info("[ThoughtService] 세션 삭제 완료 - id: {}", sessionId);
    }

    // ──────────────────────────────────────────────
    // 질문 추가 + AI 답변 생성 (QUESTION / REVISION 공통)
    // ──────────────────────────────────────────────

    @Transactional
    public AskResponse addQuestionAndGetAnswer(Long sessionId, AddQuestionRequest request) {
        // 비즈니스 레벨 검증: AI_ANSWER·INSIGHT는 클라이언트가 직접 지정 불가
        request.validateNodeType();

        LearningSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("세션을 찾을 수 없습니다. id=" + sessionId));

        ThoughtNode parentNode = resolveParentNode(request.parentNodeId(), sessionId);
        NodeType resolvedType  = request.resolvedNodeType();

        // 질문/수정 노드 저장 (Gemini 실패와 무관하게 항상 보존)
        ThoughtNode savedQuestion = nodeRepository.save(
                ThoughtNode.builder()
                        .session(session)
                        .parentNode(parentNode)
                        .nodeType(resolvedType)
                        .content(request.content())
                        .summary(null)
                        .build()
        );
        log.info("[ThoughtService] {} 노드 저장 완료 - nodeId: {}", resolvedType, savedQuestion.getId());

        // AI 답변 생성 — 실패해도 질문 노드는 롤백되지 않음 (try-catch로 트랜잭션 유지)
        String prompt = buildAnswerPrompt(sessionId, savedQuestion, resolvedType);
        Map<String, String> structured = callGeminiSafely(prompt, "AI 답변을 일시적으로 생성하지 못했습니다. 잠시 후 다시 시도해 주세요.");

        ThoughtNode savedAnswer = nodeRepository.save(
                ThoughtNode.builder()
                        .session(session)
                        .parentNode(savedQuestion)
                        .nodeType(NodeType.AI_ANSWER)
                        .content(structured.getOrDefault("answer", ""))
                        .summary(blankToNull(structured.getOrDefault("summary", "")))
                        .build()
        );
        log.info("[ThoughtService] AI 답변 노드 저장 완료 - nodeId: {}", savedAnswer.getId());

        // 세션 최신 활동 시각 갱신
        session.touch();
        sessionRepository.save(session);

        return new AskResponse(
                ThoughtNodeResponse.from(savedQuestion),
                ThoughtNodeResponse.from(savedAnswer)
        );
    }

    // ──────────────────────────────────────────────
    // 학습 회고 인사이트 생성 (세션 전체 분석)
    // ──────────────────────────────────────────────

    @Transactional
    public InsightResponse createInsight(Long sessionId) {
        LearningSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("세션을 찾을 수 없습니다. id=" + sessionId));

        List<ThoughtNode> allNodes = nodeRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);

        long questionCount = allNodes.stream()
                .filter(n -> n.getNodeType() == NodeType.QUESTION || n.getNodeType() == NodeType.REVISION)
                .count();
        if (questionCount == 0) {
            throw new IllegalStateException("회고 인사이트를 생성하려면 최소 1개의 질문이 필요합니다.");
        }

        log.info("[ThoughtService] 학습 회고 인사이트 생성 시작 - sessionId: {}, 노드 수: {}", sessionId, allNodes.size());
        String prompt = buildInsightPrompt(session.getTitle(), allNodes);
        // 인사이트는 실패 시 롤백 (저장하지 않음) — 사용자가 다시 시도 가능
        Map<String, String> structured = geminiClient.generateStructuredContent(prompt);

        ThoughtNode insightNode = nodeRepository.save(
                ThoughtNode.builder()
                        .session(session)
                        .parentNode(null)
                        .nodeType(NodeType.INSIGHT)
                        .content(structured.getOrDefault("answer", "인사이트를 생성하지 못했습니다."))
                        .summary(Optional.ofNullable(blankToNull(structured.getOrDefault("summary", "")))
                                .orElse("학습 회고"))
                        .build()
        );

        session.touch();
        sessionRepository.save(session);

        log.info("[ThoughtService] INSIGHT 노드 저장 완료 - nodeId: {}", insightNode.getId());
        return new InsightResponse(ThoughtNodeResponse.from(insightNode));
    }

    // ──────────────────────────────────────────────────────────────
    // 마인드맵 트리 조회 (depth 포함)
    // ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ThoughtTreeNode> getTree(Long sessionId) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new EntityNotFoundException("세션을 찾을 수 없습니다. id=" + sessionId);
        }

        List<ThoughtNode> allNodes = nodeRepository.findAllWithChildrenBySessionId(sessionId);
        if (allNodes.isEmpty()) {
            return Collections.emptyList();
        }

        // 자식 노드 맵 구성 (parentId → List<ThoughtNode>)
        Map<Long, List<ThoughtNode>> childrenMap = new HashMap<>();
        List<ThoughtNode> rootNodes = new ArrayList<>();

        for (ThoughtNode node : allNodes) {
            if (node.getParentNode() == null) {
                rootNodes.add(node);
            } else {
                childrenMap
                        .computeIfAbsent(node.getParentNode().getId(), k -> new ArrayList<>())
                        .add(node);
            }
        }

        // 루트에서 재귀적으로 트리 구성 (depth 자연스럽게 계산)
        List<ThoughtTreeNode> result = rootNodes.stream()
                .sorted(Comparator.comparing(ThoughtNode::getCreatedAt))
                .map(root -> buildTreeNode(root, childrenMap, 0))
                .collect(Collectors.toList());

        log.info("[ThoughtService] 트리 조회 완료 - sessionId: {}, 전체 노드 수: {}, 루트 수: {}",
                sessionId, allNodes.size(), result.size());
        return result;
    }

    // ──────────────────────────────────────────────────────────────
    // 내부: 재귀 트리 빌더
    // ──────────────────────────────────────────────────────────────

    private ThoughtTreeNode buildTreeNode(ThoughtNode node,
                                          Map<Long, List<ThoughtNode>> childrenMap,
                                          int depth) {
        List<ThoughtTreeNode> childDTOs = childrenMap
                .getOrDefault(node.getId(), Collections.emptyList())
                .stream()
                .sorted(Comparator.comparing(ThoughtNode::getCreatedAt))
                .map(child -> buildTreeNode(child, childrenMap, depth + 1))
                .collect(Collectors.toList());
        return ThoughtTreeNode.from(node, childDTOs, depth);
    }

    // ──────────────────────────────────────────────────────────────
    // 내부: 소크라테스식 AI 답변 프롬프트 생성
    // ──────────────────────────────────────────────────────────────

    private String buildAnswerPrompt(Long sessionId, ThoughtNode currentQuestion, NodeType type) {
        List<ThoughtNode> previousNodes = nodeRepository
                .findBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .filter(n -> !n.getId().equals(currentQuestion.getId()))
                .filter(n -> n.getNodeType() != NodeType.INSIGHT)
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append("""
                당신은 학생의 자기주도 학습을 돕는 소크라테스식 AI 튜터입니다.
                단순히 정답을 알려주는 것이 아니라, 학생이 스스로 생각하고 탐구하며 깊은 이해에 도달하도록 안내합니다.

                """);

        if (!previousNodes.isEmpty()) {
            sb.append("=== 이번 학습 세션의 대화 흐름 ===\n");
            for (ThoughtNode node : previousNodes) {
                switch (node.getNodeType()) {
                    case QUESTION  -> sb.append("[학생 질문] ").append(node.getContent());
                    case AI_ANSWER -> {
                        String label = (node.getSummary() != null && !node.getSummary().isBlank())
                                ? node.getSummary()
                                : truncate(node.getContent(), 200);
                        sb.append("[AI 답변 요약] ").append(label);
                    }
                    case REVISION  -> sb.append("[학생 이해 수정] ").append(node.getContent());
                    default        -> {} // INSIGHT는 이미 필터링됨
                }
                sb.append("\n");
            }
            sb.append("================================\n\n");
        }

        if (type == NodeType.REVISION) {
            sb.append("""
                    [학생의 이해 수정 / 심화 탐색]
                    학생이 이전 학습을 바탕으로 자신의 이해를 수정하거나 더 깊이 탐구하고 있습니다.
                    변화한 이해를 긍정적으로 인정하고, 다음 단계의 탐구를 격려해주세요.

                    """);
        } else {
            sb.append("[학생의 현재 질문]\n");
        }
        sb.append(currentQuestion.getContent()).append("\n\n");

        sb.append("""
                아래 JSON 형식으로만 응답하세요 (코드블록·마크다운 없이 순수 JSON):
                {
                  "summary": "이 답변의 핵심을 한 문장으로 (마인드맵 노드 라벨용, 30자 이내, 한국어)",
                  "answer": "교육적 답변 (아래 지침 준수, answer 필드 안은 마크다운 가능)"
                }

                [답변 작성 지침]
                1. 학생이 이미 알고 있는 내용과 연결하여 시작하세요.
                2. 핵심 개념을 명확하고 간결하게 설명하세요 (예시 포함 권장).
                3. 답변 마지막에 학생의 사고를 심화시킬 **후속 탐구 질문**을 하나 제시하세요.
                   예: "그렇다면 ○○은 왜 ○○하게 동작할까요? 생각해보세요."
                4. 전체 답변은 한국어로 작성하세요.
                """);

        return sb.toString();
    }

    // ──────────────────────────────────────────────────────────────
    // 내부: 학습 회고 인사이트 프롬프트 생성
    // ──────────────────────────────────────────────────────────────

    private String buildInsightPrompt(String sessionTitle, List<ThoughtNode> nodes) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                당신은 학습 여정을 분석하는 교육 전문가입니다.
                아래는 한 학생이 AI 튜터와 함께 진행한 탐구 학습의 전체 기록입니다.

                """);
        sb.append("=== 학습 주제: ").append(sessionTitle).append(" ===\n\n");

        for (ThoughtNode node : nodes) {
            if (node.getNodeType() == NodeType.INSIGHT) continue;
            switch (node.getNodeType()) {
                case QUESTION  -> sb.append("[학생 질문] ").append(node.getContent());
                case AI_ANSWER -> sb.append("[AI 설명] ").append(truncate(node.getContent(), 300));
                case REVISION  -> sb.append("[학생 이해 수정] ").append(node.getContent());
                default        -> {}
            }
            sb.append("\n\n");
        }

        sb.append("""
                ==============================

                위 학습 여정 전체를 분석하여 아래 JSON 형식으로만 응답하세요 (코드블록 없이 순수 JSON):
                {
                  "summary": "이 학습 회고의 핵심 한 줄 요약 (30자 이내, 한국어)",
                  "answer": "학습 회고 리포트 전문 (아래 6가지 항목 포함, 마크다운 사용, 한국어)"
                }

                [회고 리포트에 반드시 포함할 6가지 항목]

                ## 1. 학습 출발점
                처음 이 주제에 대해 어떤 궁금증을 가지고 시작했는지, 초기 이해 수준을 짧게 서술하세요.

                ## 2. 사고의 여정
                어떤 순서로 개념을 탐구했고, 어느 지점에서 막혔거나 이해가 깊어졌는지 서술하세요.

                ## 3. 이해의 변화
                학습 전과 후의 개념 이해가 어떻게 달라졌는지, 구체적인 예를 들어 설명하세요.

                ## 4. 핵심 연결고리
                이번 세션에서 발견한 개념들 사이의 연결 관계(2~3개)를 정리하세요.
                예: '광합성 → 엽록체 → 틸라코이드막'

                ## 5. 성장 포인트
                학생이 특히 잘 이해한 부분을 구체적으로 칭찬하고, 더 발전시키면 좋을 부분을 제안하세요.

                ## 6. 다음 탐구 방향
                이 학습을 바탕으로 자연스럽게 이어질 탐구 주제 2~3가지를 추천하세요.

                학생이 자신의 학습을 자랑스럽게 돌아볼 수 있도록 긍정적이고 구체적으로 작성해주세요.
                """);
        return sb.toString();
    }

    // ──────────────────────────────────────────────
    // 유틸
    // ──────────────────────────────────────────────

    /** parentNodeId가 있으면 노드를 조회하고 세션 소속 검증, null이면 null 반환 */
    private ThoughtNode resolveParentNode(Long parentNodeId, Long sessionId) {
        if (parentNodeId == null) return null;
        ThoughtNode parent = nodeRepository.findById(parentNodeId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "부모 노드를 찾을 수 없습니다. id=" + parentNodeId));
        if (!parent.getSession().getId().equals(sessionId)) {
            throw new IllegalArgumentException(
                    "부모 노드가 현재 세션에 속하지 않습니다. nodeId=" + parentNodeId);
        }
        return parent;
    }

    /**
     * Gemini API 호출. 실패 시 예외 대신 fallbackMessage를 answer로 하는 Map 반환.
     * Q&A 흐름에서 질문 노드가 롤백되지 않도록 트랜잭션을 유지한다.
     */
    private Map<String, String> callGeminiSafely(String prompt, String fallbackMessage) {
        try {
            return geminiClient.generateStructuredContent(prompt);
        } catch (Exception e) {
            log.error("[ThoughtService] Gemini 호출 실패 - 폴백 적용. 원인: {}", e.getMessage());
            return Map.of("summary", "AI 응답 실패", "answer", fallbackMessage);
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
