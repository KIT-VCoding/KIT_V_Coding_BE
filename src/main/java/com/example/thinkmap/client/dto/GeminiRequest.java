package com.example.thinkmap.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

/**
 * Gemini API generateContent 요청 바디
 * POST /v1beta/models/{model}:generateContent?key={apiKey}
 */
@Builder
public record GeminiRequest(
        List<Content> contents,
        @JsonProperty("generationConfig") GenerationConfig generationConfig
) {

    @Builder
    public record Content(
            String role,
            List<Part> parts
    ) {}

    @Builder
    public record Part(
            String text
    ) {}

    @Builder
    public record GenerationConfig(
            @JsonProperty("maxOutputTokens") int maxOutputTokens,
            double temperature
    ) {}

    /** 일반 텍스트 응답용 */
    public static GeminiRequest of(String prompt) {
        return GeminiRequest.builder()
                .contents(List.of(
                        Content.builder()
                                .role("user")
                                .parts(List.of(Part.builder().text(prompt).build()))
                                .build()
                ))
                .generationConfig(GenerationConfig.builder()
                        .maxOutputTokens(2048)
                        .temperature(0.7)
                        .build())
                .build();
    }

    /**
     * 구조화 JSON 응답용 (summary + answer).
     * - maxOutputTokens 4096: 인사이트 리포트처럼 긴 응답 허용
     * - temperature 0.3: JSON 형식 안정성 확보 (낮을수록 형식 일관성 높아짐)
     */
    public static GeminiRequest ofStructured(String prompt) {
        return GeminiRequest.builder()
                .contents(List.of(
                        Content.builder()
                                .role("user")
                                .parts(List.of(Part.builder().text(prompt).build()))
                                .build()
                ))
                .generationConfig(GenerationConfig.builder()
                        .maxOutputTokens(4096)
                        .temperature(0.3)
                        .build())
                .build();
    }
}
