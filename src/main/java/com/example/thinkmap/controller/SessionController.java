package com.example.thinkmap.controller;

import com.example.thinkmap.dto.CreateSessionRequest;
import com.example.thinkmap.dto.SessionResponse;
import com.example.thinkmap.dto.UpdateSessionTitleRequest;
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

@Tag(name = "학습 세션 관리", description = "학습 세션 생성·목록·상세 조회·제목 수정·삭제. 세션은 하나의 주제를 탐구하는 단위입니다.")
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final ThoughtService thoughtService;

    @Operation(summary = "학습 세션 생성",
               description = "새로운 학습 탐구 세션을 생성합니다. 탐구할 주제·질문을 title로 설정하세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "세션 생성 성공"),
            @ApiResponse(responseCode = "400", description = "title 누락 또는 255자 초과")
    })
    @PostMapping
    public ResponseEntity<SessionResponse> createSession(
            @Valid @RequestBody CreateSessionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(thoughtService.createSession(request, principal.getId()));
    }

    @Operation(summary = "학습 세션 목록 조회",
               description = "로그인한 사용자의 학습 세션을 최신 활동 순으로 반환합니다.")
    @ApiResponse(responseCode = "200", description = "세션 목록 반환 성공")
    @GetMapping
    public ResponseEntity<List<SessionResponse>> listSessions(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(thoughtService.listSessions(principal.getId()));
    }

    @Operation(summary = "학습 세션 상세 조회",
               description = "특정 세션의 상세 정보(노드 수, 질문 수, 인사이트 여부 등)를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "세션 상세 반환 성공"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음")
    })
    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> getSession(
            @Parameter(description = "학습 세션 ID") @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(thoughtService.getSession(id, principal.getId()));
    }

    @Operation(summary = "세션 제목 수정",
               description = "세션 제목을 변경합니다. updatedAt도 함께 갱신됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "제목 수정 성공"),
            @ApiResponse(responseCode = "400", description = "title 누락 또는 255자 초과"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<SessionResponse> updateSessionTitle(
            @Parameter(description = "학습 세션 ID") @PathVariable Long id,
            @Valid @RequestBody UpdateSessionTitleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(thoughtService.updateSessionTitle(id, request, principal.getId()));
    }

    @Operation(summary = "학습 세션 삭제",
               description = "세션과 포함된 모든 노드(질문·답변·인사이트)를 영구 삭제합니다. 복구 불가.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(
            @Parameter(description = "학습 세션 ID") @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        thoughtService.deleteSession(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
