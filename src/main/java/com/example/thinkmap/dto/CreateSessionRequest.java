package com.example.thinkmap.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "학습 세션 생성 요청")
public record CreateSessionRequest(
        @Schema(description = "세션 제목 (탐구할 주제·질문)", example = "광합성 탐구")
        @NotBlank(message = "세션 제목은 필수입니다.")
        @Size(max = 255, message = "세션 제목은 255자를 초과할 수 없습니다.")
        String title
) {
    /** 입력값 정규화 (앞뒤 공백 제거) */
    public CreateSessionRequest {
        if (title != null) title = title.strip();
    }
}
