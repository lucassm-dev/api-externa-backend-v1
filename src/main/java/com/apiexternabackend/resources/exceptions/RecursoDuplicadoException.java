package com.apiexternabackend.resources.exceptions;

import org.springframework.http.HttpStatus;

public class RecursoDuplicadoException extends NegocioException {

    public RecursoDuplicadoException(String codigo, String mensagem) {
        super(codigo, mensagem, HttpStatus.CONFLICT);
    }
}