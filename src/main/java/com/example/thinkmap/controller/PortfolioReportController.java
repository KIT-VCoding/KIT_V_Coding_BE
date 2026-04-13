package com.example.thinkmap.controller;

import com.example.thinkmap.dto.PortfolioReportRequest;
import com.example.thinkmap.dto.PortfolioReportResponse;
import com.example.thinkmap.security.UserPrincipal;
import com.example.thinkmap.service.PortfolioReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "학습 포트폴리오", description = "세션 로그 기반 AI 포트폴리오 보고서 생성")
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class PortfolioReportController {

    private final PortfolioReportService portfolioReportService;

    @Operation(
            summary = "AI 학습 포트폴리오 보고서 생성",
            description = """
                    세션 ID에 해당하는 저장된 학습 노드(질문·AI답변·이해수정·선택적 인사이트)를 입력으로 하여
                    포트폴리오 보고서 JSON을 반환합니다. 프론트는 응답 필드를 그대로 보고서 UI에 매핑하면 됩니다.

                    선택 본문 `analysisJson`에 응답과 동일한 키(`reportTitle` 등)가 하나라도 있으면 Gemini 호출 없이 변환합니다.
                    Swagger 기본값처럼 `additionalProp*`만 있으면 무시하고 세션 로그로 생성합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "보고서 생성 성공"),
            @ApiResponse(responseCode = "400", description = "질문 노드 없음, JSON 파싱 실패 등"),
            @ApiResponse(responseCode = "404", description = "세션 없음"),
            @ApiResponse(responseCode = "500", description = "Gemini API 오류 등")
    })
    @PostMapping("/{sessionId}/portfolio-report")
    public ResponseEntity<PortfolioReportResponse> generatePortfolioReport(
            @Parameter(description = "학습 세션 ID") @PathVariable Long sessionId,
            @RequestBody(required = false) PortfolioReportRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(portfolioReportService.generateReport(sessionId, request, principal.getId()));
    }
}
