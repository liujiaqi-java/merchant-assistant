package com.merchantassistant.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ProductNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiError> handleValidation(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "请求参数不合法");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleConflict(IllegalArgumentException exception) {
        return error(HttpStatus.BAD_REQUEST, safeMessage(exception.getMessage(), "请求无法处理"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "服务器暂时无法完成请求，请稍后重试");
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ApiError(Instant.now(), status.value(), message));
    }

    private String safeMessage(String message, String fallback) {
        return message == null || message.isBlank() ? fallback : message;
    }
}
