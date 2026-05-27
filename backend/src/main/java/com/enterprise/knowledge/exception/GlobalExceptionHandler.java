package com.enterprise.knowledge.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex) {
        return buildErrorResponse(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        return buildErrorResponse("ERROR", ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(String code, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", Instant.now());
        error.put("code", code);
        error.put("message", message);
        return ResponseEntity.badRequest().body(error);
    }
}
