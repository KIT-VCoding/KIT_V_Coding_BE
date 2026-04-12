package com.example.thinkmap.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "BearerAuth";

    @Bean
    public OpenAPI openAPI(@Value("${app.api-public-url:}") String apiPublicUrl) {
        OpenAPI api = new OpenAPI()
                .info(new Info()
                        .title("ThinkMap API")
                        .description("""
                                **사고과정 시각화 학습 AI** 백엔드 API

                                ---

                                ## 로그인 & Swagger 인증 방법

                                ### 1. 자체 회원가입 / 로그인
                                1. `Auth` 태그 → `POST /api/auth/signup` 으로 회원가입
                                2. 또는 `POST /api/auth/login` 으로 로그인
                                3. 응답의 `accessToken` 값 복사
                                4. 우측 상단 **Authorize 🔒** 버튼 클릭 → `Bearer <토큰>` 없이 토큰만 붙여넣기 → Authorize

                                ### 2. 카카오 소셜 로그인
                                1. `Auth` 태그 → `GET /api/auth/kakao/url` 실행
                                2. 응답의 `loginUrl` 을 **브라우저 새 탭**에 붙여넣고 이동
                                3. 카카오 로그인 완료 → 리다이렉트 URL의 `?token=...` 값 복사
                                4. 우측 상단 **Authorize 🔒** 버튼 클릭 → 토큰 붙여넣기 → Authorize

                                ---

                                ## 주요 노드 타입
                                - `QUESTION` : 학생의 질문
                                - `AI_ANSWER` : AI의 소크라테스식 교육적 답변
                                - `REVISION`  : 학생의 이해 수정 / 심화 탐색
                                - `INSIGHT`   : AI가 세션 전체를 분석한 학습 회고 리포트
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("ThinkMap Team"))
                )
                // JWT Bearer 인증 스킴 등록
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT 토큰을 입력하세요. 'Bearer ' 접두사 없이 토큰만 입력하면 됩니다.")
                        )
                )
                // 전역 보안 요구사항 — 모든 엔드포인트에 자물쇠 아이콘 표시
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));

        String base = apiPublicUrl == null ? "" : apiPublicUrl.strip();
        if (!base.isEmpty()) {
            if (base.endsWith("/")) {
                base = base.substring(0, base.length() - 1);
            }
            // 리버스 프록시 뒤에서도 Swagger Try-it-out이 https 공개 URL로 나가도록 고정
            api.setServers(List.of(new Server().url(base).description("Public API (HTTPS)")));
        }
        return api;
    }
}
