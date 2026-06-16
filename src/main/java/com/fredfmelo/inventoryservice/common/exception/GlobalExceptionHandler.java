package com.fredfmelo.inventoryservice.common.exception;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.fredfmelo.eventdrivencore.exception.BusinessException;
import com.fredfmelo.eventdrivencore.exception.TechnicalException;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        log.error(ex.getMessage(), ex);

        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                ex.getHttpStatusCode(),
                ex.getMessage());

        return ResponseEntity
                .status(HttpStatusCode.valueOf(ex.getHttpStatusCode()))
                .body(response);
    }

    @ExceptionHandler(TechnicalException.class)
    public ResponseEntity<ErrorResponse> handleTechnical(TechnicalException ex) {
        log.error(ex.getMessage(), ex);

        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                ex.getHttpStatusCode(),
                ex.getMessage());

        return ResponseEntity
                .status(HttpStatusCode.valueOf(ex.getHttpStatusCode()))
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");

        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                message);

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Invalid request parameter: " + ex.getName());

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);

        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                500,
                "Internal server error");

        return ResponseEntity
                .internalServerError()
                .body(response);
    }
}
