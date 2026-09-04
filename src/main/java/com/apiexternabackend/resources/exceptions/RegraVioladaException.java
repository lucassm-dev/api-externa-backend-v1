package com.apiexternabackend.resources.exceptions;

import org.springframework.http.HttpStatus;

public class RegraVioladaException extends NegocioException {

    public RegraVioladaException(String codigo, String mensagem) {
        super(codigo, mensagem, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}