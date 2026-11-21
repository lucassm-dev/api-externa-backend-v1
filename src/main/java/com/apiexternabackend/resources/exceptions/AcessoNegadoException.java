package com.apiexternabackend.resources.exceptions;

import org.springframework.http.HttpStatus;

public class AcessoNegadoException extends NegocioException {

    public AcessoNegadoException(String codigo, String mensagem) {
        super(codigo, mensagem, HttpStatus.FORBIDDEN);
    }
}
