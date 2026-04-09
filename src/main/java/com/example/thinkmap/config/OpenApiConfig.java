package com.example.thinkmap.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ThinkMap API")
                        .description("""
                                **사고과정 시각화 학습 AI** 백엔드 API

                                학생이 AI와 학습하는 과정에서 '처음 질문 → 탐구 → 이해 변화'의
                                전체 흐름을 노드 트리로 저장하고, 마인드맵으로 시각화하며,
                                AI가 학습 회고 인사이트를 생성합니다.

                                **주요 노드 타입**
                                - `QUESTION` : 학생의 질문
                                - `AI_ANSWER` : AI의 소크라테스식 교육적 답변
                                - `REVISION`  : 학생의 이해 수정 / 심화 탐색
                                - `INSIGHT`   : AI가 세션 전체를 분석한 학습 회고 리포트
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ThinkMap Team")
                        )
                );
    }
}
