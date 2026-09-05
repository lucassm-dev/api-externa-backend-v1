package com.apiexternabackend.resources.exceptions;

import org.springframework.http.HttpStatus;

public class CredenciaisInvalidasException extends NegocioException {

    public CredenciaisInvalidasException(String codigo, String mensagem) {
        super(codigo, mensagem, HttpStatus.UNAUTHORIZED);
    }
}