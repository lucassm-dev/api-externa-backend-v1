package com.apiexternabackend.resources.exceptions;

import org.springframework.http.HttpStatus;

public class IntegracaoExternaException extends NegocioException {

    private final boolean limiteExcedido;

    public IntegracaoExternaException(String codigo, String mensagem, boolean limiteExcedido) {
        super(codigo, mensagem, limiteExcedido ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.SERVICE_UNAVAILABLE);
        this.limiteExcedido = limiteExcedido;
    }

    public IntegracaoExternaException(String codigo, String mensagem, boolean limiteExcedido, Throwable cause) {
        super(codigo, mensagem, limiteExcedido ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.SERVICE_UNAVAILABLE, cause);
        this.limiteExcedido = limiteExcedido;
    }

    public boolean isLimiteExcedido() {
        return limiteExcedido;
    }
}