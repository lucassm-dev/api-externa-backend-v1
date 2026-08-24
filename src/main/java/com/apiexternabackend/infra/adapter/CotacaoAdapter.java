package com.apiexternabackend.infra.adapter;

/**
 * Abstração comum de cotação (Item 15 — isolamento de serviço externo).
 * Cada mercado resolve para uma implementação diferente.
 */
public interface CotacaoAdapter {

    /**
     * Busca a cotação atual do ticker.
     *
     * @throws com.apiexternabackend.resources.exceptions.BusinessException  se o ticker não existir na fonte
     * @throws com.apiexternabackend.resources.exceptions.ExternalServiceException se a fonte estiver fora do ar
     */
    CotacaoResultado buscarCotacao(String ticker);
}
