package com.apiexternabackend.resources.exceptions;

import org.springframework.http.HttpStatus;

public abstract class NegocioException extends RuntimeException {

    private final String codigo;
    private final HttpStatus httpStatus;

    protected NegocioException(String codigo, String mensagem, HttpStatus httpStatus) {
        super(mensagem);
        this.codigo = codigo;
        this.httpStatus = httpStatus;
    }

    protected NegocioException(String codigo, String mensagem, HttpStatus httpStatus, Throwable cause) {
        super(mensagem, cause);
        this.codigo = codigo;
        this.httpStatus = httpStatus;
    }

    public String getCodigo() {
        return codigo;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
