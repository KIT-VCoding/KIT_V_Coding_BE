package com.example.thinkmap.controller;

import com.example.thinkmap.dto.AddQuestionRequest;
import com.example.thinkmap.dto.AskResponse;
import com.example.thinkmap.dto.InsightResponse;
import com.example.thinkmap.dto.ThoughtTreeNode;
import com.example.thinkmap.security.UserPrincipal;
import com.example.thinkmap.service.ThoughtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "학습 노드 & 마인드맵", description = "질문·수정 노드 추가, 학습 회고 인사이트 생성, 마인드맵 트리 조회")
@RestController
@RequestMapping("/api/sessions/{sessionId}/nodes")
@RequiredArgsConstructor
public class ThoughtController {

    private final ThoughtService thoughtService;

    @Operation(
            summary = "질문 / 이해 수정 노드 추가",
            description = """
                    학생의 질문(QUESTION) 또는 이해 수정(REVISION)을 노드로 저장하고,
                    AI 소크라테스식 답변을 함께 반환합니다.
                    REVISION 모드에서는 AI가 이해 변화를 인정하고 심화 탐구를 격려합니다.
                    응답의 answerNode.summary 필드를 마인드맵 노드 라벨로 사용하세요.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "노드 생성 및 AI 답변 반환 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 유효성 오류 (content 누락, 잘못된 nodeType 등)"),
            @ApiResponse(responseCode = "404", description = "세션 또는 부모 노드를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "Gemini API 호출 실패")
    })
    @PostMapping
    public ResponseEntity<AskResponse> addQuestion(
            @Parameter(description = "학습 세션 ID") @PathVariable Long sessionId,
            @Valid @RequestBody AddQuestionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(thoughtService.addQuestionAndGetAnswer(sessionId, request, principal.getId()));
    }

    @Operation(
            summary = "학습 회고 인사이트 생성",
            description = """
                    세션 전체 학습 여정(질문·답변·이해수정 흐름)을 AI가 분석하여
                    6항목 회고 리포트(학습 출발점·사고 여정·이해 변화·핵심 연결고리·성장 포인트·다음 탐구 방향)를
                    INSIGHT 노드로 생성합니다.
                    최소 1개의 질문 노드가 존재해야 호출 가능합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "인사이트 노드 생성 성공"),
            @ApiResponse(responseCode = "400", description = "질문 노드가 없어 인사이트 생성 불가"),
            @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "Gemini API 호출 실패")
    })
    @PostMapping("/insight")
    public ResponseEntity<InsightResponse> createInsight(
            @Parameter(description = "학습 세션 ID") @PathVariable Long sessionId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(thoughtService.createInsight(sessionId, principal.getId()));
    }

    @Operation(
            summary = "마인드맵 트리 조회",
            description = """
                    세션의 전체 ThoughtNode를 계층형(Tree) JSON으로 반환합니다.
                    프론트엔드 마인드맵 렌더링에 사용합니다.
                    각 노드의 nodeType(QUESTION·AI_ANSWER·REVISION·INSIGHT)과
                    summary 필드를 활용하여 노드 스타일을 구분하세요.
                    INSIGHT 노드는 최상위(parentNodeId=null)에 위치하며 다른 노드와 시각적으로 구별됩니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "트리 조회 성공"),
            @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음")
    })
    @GetMapping("/tree")
    public ResponseEntity<List<ThoughtTreeNode>> getTree(
            @Parameter(description = "학습 세션 ID") @PathVariable Long sessionId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(thoughtService.getTree(sessionId, principal.getId()));
    }
}
