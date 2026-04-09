package com.example.thinkmap.client;

import com.example.thinkmap.client.dto.GeminiRequest;
import com.example.thinkmap.client.dto.GeminiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiClient {

    private final WebClient geminiWebClient;
    private final ObjectMapper objectMapper;  // Spring Boot auto-config 빈 주입

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model}")
    private String model;

    /**
     * Gemini API에 프롬프트를 전송하고 응답 텍스트를 반환한다.
     */
    public String generateContent(String prompt) {
        return callApi(GeminiRequest.of(prompt));
    }

    /**
     * Gemini API에 프롬프트를 전송하고 구조화된 응답(summary + answer)을 파싱하여 반환한다.
     * 프롬프트가 JSON 형식 응답을 요청하는 경우에 사용한다.
     *
     * @return Map with keys "summary" and "answer". 파싱 실패 시 "answer" = raw 텍스트로 폴백.
     */
    public Map<String, String> generateStructuredContent(String prompt) {
        String raw = callApi(GeminiRequest.ofStructured(prompt));
        return parseStructuredResponse(raw);
    }

    // ──────────────────────────────────────────────
    // 내부: 공통 API 호출
    // ──────────────────────────────────────────────

    private String callApi(GeminiRequest request) {
        int promptLen = safePromptLength(request);
        log.debug("[GeminiClient] 요청 모델: {}, 프롬프트 길이: {}자", model, promptLen);

        try {
            GeminiResponse response = geminiWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(model))
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GeminiResponse.class)
                    // 503(서버 과부하)·429(Rate Limit) 최대 3회 지수 백오프 재시도
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                            .maxBackoff(Duration.ofSeconds(16))
                            .filter(this::isRetryableError)
                            .doBeforeRetry(s -> log.warn("[GeminiClient] 재시도 {}/3...", s.totalRetries() + 1)))
                    .block();

            if (response == null) {
                log.warn("[GeminiClient] 응답이 null입니다.");
                return "AI 응답을 받지 못했습니다.";
            }

            String result = response.extractText();
            log.debug("[GeminiClient] 응답 수신 완료, 길이: {}자", result.length());
            return result;

        } catch (WebClientResponseException e) {
            log.error("[GeminiClient] API 오류 - status: {}, body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            String userMessage = switch (e.getStatusCode().value()) {
                case 400 -> "요청 형식이 잘못되었습니다.";
                case 401, 403 -> "Gemini API 키가 유효하지 않습니다.";
                case 429 -> "AI 요청 한도를 초과했습니다. 잠시 후 다시 시도해 주세요.";
                case 500, 503 -> "Gemini 서버에 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.";
                default -> "Gemini API 호출에 실패했습니다.";
            };
            throw new RuntimeException(userMessage, e);
        } catch (Exception e) {
            log.error("[GeminiClient] 예상치 못한 오류 발생", e);
            throw new RuntimeException("AI 서비스에 연결하는 중 오류가 발생했습니다.", e);
        }
    }

    /** 재시도 가능한 오류 여부 판별 (503 서버 과부하, 429 Rate Limit) */
    private boolean isRetryableError(Throwable t) {
        if (t instanceof WebClientResponseException e) {
            int code = e.getStatusCode().value();
            return code == 503 || code == 429;
        }
        return false;
    }

    // ──────────────────────────────────────────────
    // 내부: 구조화 응답 파싱 (JSON → summary / answer)
    // ──────────────────────────────────────────────

    private Map<String, String> parseStructuredResponse(String raw) {
        try {
            // Gemini가 ```json ... ``` 코드블록으로 감쌀 수 있으므로 제거
            String cleaned = raw.replaceAll("(?s)```json\\s*", "").replaceAll("```", "").strip();

            // 첫 번째 '{' ~ 마지막 '}' 사이만 추출 (앞뒤 불필요한 텍스트 제거)
            int start = cleaned.indexOf('{');
            int end   = cleaned.lastIndexOf('}');
            if (start >= 0 && end > start) {
                cleaned = cleaned.substring(start, end + 1);
            } else {
                log.warn("[GeminiClient] JSON 블록을 찾을 수 없음 - raw 텍스트로 폴백");
                return Map.of("summary", "", "answer", raw);
            }

            JsonNode root    = objectMapper.readTree(cleaned);
            String summary   = root.path("summary").asText("").strip();
            String answer    = root.path("answer").asText("").strip();

            if (answer.isEmpty()) {
                // answer 필드가 비어있으면 raw 전체를 answer로 사용
                log.warn("[GeminiClient] answer 필드가 비어있음 - raw 텍스트로 폴백");
                return Map.of("summary", summary, "answer", raw);
            }
            return Map.of("summary", trimSummary(summary), "answer", answer);

        } catch (Exception e) {
            log.warn("[GeminiClient] JSON 파싱 실패 - raw 텍스트로 폴백. 원인: {}", e.getMessage());
            return Map.of("summary", "", "answer", raw);
        }
    }

    private String trimSummary(String summary) {
        if (summary == null || summary.isBlank()) return "";
        return summary.length() > 80 ? summary.substring(0, 77) + "..." : summary;
    }

    private int safePromptLength(GeminiRequest request) {
        try {
            return request.contents().get(0).parts().get(0).text().length();
        } catch (Exception e) {
            return 0;
        }
    }
}
