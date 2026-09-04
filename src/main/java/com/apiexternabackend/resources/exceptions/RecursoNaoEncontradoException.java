package com.apiexternabackend.resources.exceptions;

import org.springframework.http.HttpStatus;

public class RecursoNaoEncontradoException extends NegocioException {

    public RecursoNaoEncontradoException(String codigo, String mensagem) {
        super(codigo, mensagem, HttpStatus.NOT_FOUND);
    }
}
