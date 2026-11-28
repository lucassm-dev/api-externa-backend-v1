package com.apiexternabackend.resources.exceptions;

import org.springframework.web.HttpRequestMethodNotSupportedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    // spec-27: parâmetro de URL com tipo errado (ex.: tipo=TROCA) é erro do cliente, não erro interno
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<StandardError> handleParametroInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        StandardError err = new StandardError(
                Instant.now(), HttpStatus.BAD_REQUEST.value(), "VAL-002",
                "Parâmetro inválido", "Verifique os parâmetros informados", request.getRequestURI());
        err.addFieldError(ex.getName(), "Valor inválido: " + ex.getValue());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
    }

    // spec-25: upload acima do teto do multipart (11 MB) é barrado pelo Spring antes do serviço; responde como o limite de 10 MB
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<StandardError> handleUploadGrande(HttpServletRequest request) {
        return handleNegocio(new RegraVioladaException("AUT-016", "A foto deve ter no máximo 10 MB."), request);
    }

    // rota pública inexistente (ex.: Swagger fora do dev, AC-549) é 404, não erro interno
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleRotaInexistente() {
        return ResponseEntity.notFound().build();
    }

    // spec-21: método que a rota não aceita (ex.: POST /acoes depois da #24) é 405, não erro interno
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Void> handleMetodoNaoSuportado() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    // @PreAuthorize nega dentro do controller; relançar devolve ao filtro de segurança, que responde 403 AUT-007
    @ExceptionHandler(AccessDeniedException.class)
    public void handleAcessoNegado(AccessDeniedException ex) {
        throw ex;
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