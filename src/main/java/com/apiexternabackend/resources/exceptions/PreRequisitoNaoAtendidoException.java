package com.apiexternabackend.resources.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Ação bloqueada porque um pré-requisito de estado não foi satisfeito
 * (ex.: cadastrar ação sem ter uma carteira) — 409, distinto de duplicidade
 * (RecursoDuplicadoException) e de regra de validação de payload (RegraVioladaException, 422).
 */
public class PreRequisitoNaoAtendidoException extends NegocioException {

    public PreRequisitoNaoAtendidoException(String codigo, String mensagem) {
        super(codigo, mensagem, HttpStatus.CONFLICT);
    }
}
