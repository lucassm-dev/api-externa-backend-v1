package com.apiexternabackend.resources.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<StandardError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        StandardError err = new StandardError(
                Instant.now(), HttpStatus.NOT_FOUND.value(),
                "Recurso não encontrado", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<StandardError> handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
        StandardError err = new StandardError(
                Instant.now(), HttpStatus.CONFLICT.value(),
                "Recurso duplicado", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<StandardError> handleBusiness(BusinessException ex, HttpServletRequest request) {
        StandardError err = new StandardError(
                Instant.now(), HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "Regra de negócio violada", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(err);
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<StandardError> handleExternal(ExternalServiceException ex, HttpServletRequest request) {
        StandardError err = new StandardError(
                Instant.now(), HttpStatus.BAD_GATEWAY.value(),
                "Serviço externo indisponível", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(err);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        StandardError err = new StandardError(
                Instant.now(), HttpStatus.BAD_REQUEST.value(),
                "Dados inválidos", "Verifique os campos informados", request.getRequestURI());
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> err.addFieldError(fe.getField(), fe.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
    }
}
