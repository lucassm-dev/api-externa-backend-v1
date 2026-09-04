package com.apiexternabackend.resources.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<StandardError> handleNegocio(NegocioException ex, HttpServletRequest request) {
        StandardError err = new StandardError(
                Instant.now(), ex.getHttpStatus().value(), ex.getCodigo(),
                ex.getHttpStatus().getReasonPhrase(), ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(ex.getHttpStatus()).body(err);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        StandardError err = new StandardError(
                Instant.now(), HttpStatus.BAD_REQUEST.value(), "VAL-001",
                "Dados inválidos", "Verifique os campos informados", request.getRequestURI());
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> err.addFieldError(fe.getField(), fe.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardError> handleInesperado(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado ao processar {}", request.getRequestURI(), ex);
        StandardError err = new StandardError(
                Instant.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(), "SYS-001",
                "Erro interno", "Ocorreu um erro inesperado. Tente novamente mais tarde.", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
    }
}