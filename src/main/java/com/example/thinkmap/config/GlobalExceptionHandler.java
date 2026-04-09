package com.example.thinkmap.config;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ──────────────────────────────────────────────
    // 404 Not Found
    // ──────────────────────────────────────────────

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException e) {
        log.warn("[예외] EntityNotFound: {}", e.getMessage());
        return response(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResource(NoResourceFoundException e) {
        log.warn("[예외] NoResourceFound: {}", e.getMessage());
        return response(HttpStatus.NOT_FOUND, "요청한 경로를 찾을 수 없습니다: " + e.getResourcePath());
    }

    // ──────────────────────────────────────────────
    // 400 Bad Request
    // ──────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("[예외] Validation: {}", message);
        return response(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * 잘못된 JSON 형식, 알 수 없는 enum 값 등 요청 파싱 실패 처리.
     * 예: "nodeType": "question" (소문자), 잘못된 JSON 구조
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableMessage(HttpMessageNotReadableException e) {
        log.warn("[예외] HttpMessageNotReadable: {}", e.getMessage());
        String message = "요청 본문을 파싱할 수 없습니다. JSON 형식과 필드값을 확인해 주세요.";
        // enum 관련 오류는 더 친절한 메시지 제공
        if (e.getMessage() != null && e.getMessage().contains("not one of the values accepted for Enum class")) {
            message = "허용되지 않는 값입니다. nodeType은 QUESTION 또는 REVISION만 가능합니다.";
        }
        return response(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException e) {
        log.warn("[예외] IllegalState: {}", e.getMessage());
        return response(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("[예외] IllegalArgument: {}", e.getMessage());
        return response(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = String.format("'%s' 파라미터의 값 '%s'이(가) 올바르지 않습니다.", e.getName(), e.getValue());
        log.warn("[예외] TypeMismatch: {}", message);
        return response(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException e) {
        String message = String.format("필수 파라미터 '%s'이(가) 없습니다.", e.getParameterName());
        log.warn("[예외] MissingParam: {}", message);
        return response(HttpStatus.BAD_REQUEST, message);
    }

    // ──────────────────────────────────────────────
    // 405 Method Not Allowed
    // ──────────────────────────────────────────────

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        String message = String.format("'%s' 메서드는 지원하지 않습니다. 지원 메서드: %s",
                e.getMethod(), String.join(", ", e.getSupportedHttpMethods() != null
                        ? e.getSupportedHttpMethods().stream().map(Object::toString).collect(Collectors.toList())
                        : java.util.List.of()));
        log.warn("[예외] MethodNotSupported: {}", message);
        return response(HttpStatus.METHOD_NOT_ALLOWED, message);
    }

    // ──────────────────────────────────────────────
    // 500 Internal Server Error
    // ──────────────────────────────────────────────

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException e) {
        log.error("[예외] RuntimeException: {}", e.getMessage(), e);
        return response(HttpStatus.INTERNAL_SERVER_ERROR,
                e.getMessage() != null ? e.getMessage() : "서버 내부 오류가 발생했습니다.");
    }

    // ──────────────────────────────────────────────
    // 공통 응답 포맷
    // ──────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> response(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
