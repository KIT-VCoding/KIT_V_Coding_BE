package com.example.thinkmap.controller;

import com.example.thinkmap.domain.entity.User;
import com.example.thinkmap.domain.repository.UserRepository;
import com.example.thinkmap.dto.auth.AuthResponse;
import com.example.thinkmap.dto.auth.LoginRequest;
import com.example.thinkmap.dto.auth.SignUpRequest;
import com.example.thinkmap.dto.auth.UserInfoResponse;
import com.example.thinkmap.security.UserPrincipal;
import com.example.thinkmap.service.LocalAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 API")
public class AuthController {

    private final UserRepository userRepository;
    private final LocalAuthService localAuthService;

    // ── 자체 회원가입/로그인 (인증 불필요) ────────────────────────────

    @PostMapping("/signup")
    @SecurityRequirements   // 이 엔드포인트는 토큰 없이 호출
    @Operation(summary = "자체 회원가입",
               description = "이메일/비밀번호로 회원가입합니다. 성공 시 JWT `accessToken`을 반환합니다. 받은 토큰을 우측 상단 **Authorize 🔒** 에 입력하면 이후 API를 사용할 수 있습니다.")
    public ResponseEntity<AuthResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(localAuthService.signUp(request));
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "자체 로그인",
               description = "이메일/비밀번호로 로그인합니다. 성공 시 JWT `accessToken`을 반환합니다. 받은 토큰을 우측 상단 **Authorize 🔒** 에 입력하면 이후 API를 사용할 수 있습니다.")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(localAuthService.login(request));
    }

    // ── 카카오 소셜 로그인 URL 안내 ────────────────────────────────────

    @GetMapping("/kakao/url")
    @SecurityRequirements
    @Operation(summary = "카카오 로그인 URL 조회",
               description = """
                       카카오 OAuth2 로그인을 시작하는 URL을 반환합니다.

                       **사용 방법:**
                       1. 이 API를 실행해 `loginUrl` 값을 복사합니다.
                       2. 브라우저 새 탭에 URL을 붙여넣고 카카오 로그인을 진행합니다.
                       3. 로그인 완료 후 리다이렉트된 URL의 `?token=...` 값을 복사합니다.
                       4. 우측 상단 **Authorize 🔒** 버튼에 토큰을 입력합니다.
                       """)
    public ResponseEntity<Map<String, String>> getKakaoLoginUrl(HttpServletRequest request) {
        String baseUrl = request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() != 80 && request.getServerPort() != 443
                    ? ":" + request.getServerPort() : "");
        return ResponseEntity.ok(Map.of("loginUrl", baseUrl + "/oauth2/authorization/kakao"));
    }

    // ── 내 정보 조회 (인증 필요) ────────────────────────────────────────

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회",
               description = "JWT 토큰으로 현재 로그인한 사용자 정보를 반환합니다. Authorize 후 사용하세요.")
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
