package com.example.thinkmap.controller;

import com.example.thinkmap.domain.entity.User;
import com.example.thinkmap.domain.repository.UserRepository;
import com.example.thinkmap.dto.auth.UserInfoResponse;
import com.example.thinkmap.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 API")
public class AuthController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "JWT 토큰으로 현재 로그인한 사용자 정보를 반환합니다.")
    public ResponseEntity<UserInfoResponse> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        return ResponseEntity.ok(UserInfoResponse.from(user));
    }
}
