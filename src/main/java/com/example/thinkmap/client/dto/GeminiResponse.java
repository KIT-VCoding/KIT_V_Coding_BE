package com.example.thinkmap.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Gemini API generateContent 응답 바디
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiResponse(
        List<Candidate> candidates
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Candidate(
            Content content,
            @JsonProperty("finishReason") String finishReason
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Content(
            List<Part> parts,
            String role
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Part(
            String text
    ) {}

    /**
     * 응답에서 텍스트를 안전하게 추출
     */
    public String extractText() {
        if (candidates == null || candidates.isEmpty()) {
            return "응답을 생성하지 못했습니다.";
        }
        Candidate candidate = candidates.get(0);
        if (candidate.content() == null || candidate.content().parts() == null || candidate.content().parts().isEmpty()) {
            return "응답을 생성하지 못했습니다.";
        }
        return candidate.content().parts().get(0).text();
    }
}
