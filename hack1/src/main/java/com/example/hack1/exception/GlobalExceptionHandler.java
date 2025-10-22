package com.example.hack1.exception;

import com.example.hack1.dto.error.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ResourceConflictException ex, HttpServletRequest req) {
        ErrorResponse err = new ErrorResponse(HttpStatus.CONFLICT.value(), "Conflict", ex.getMessage(), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst()
                .orElse(ex.getMessage());
        ErrorResponse err = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation Failed", msg, req.getRequestURI());
        return ResponseEntity.badRequest().body(err);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest req) {
        ErrorResponse err = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", ex.getMessage(), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
    }
}
