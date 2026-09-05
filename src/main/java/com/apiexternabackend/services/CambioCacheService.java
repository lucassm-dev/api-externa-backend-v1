package com.apiexternabackend.services;

import com.apiexternabackend.infra.facade.CambioFacade;
import com.apiexternabackend.infra.facade.CambioResultado;
import com.apiexternabackend.resources.exceptions.IntegracaoExternaException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Cache de câmbio USD-BRL com TTL (Q-MAP-05, mesmo padrão do CotacaoCacheService).
 * É global — não por Acao — porque só existe um par de moedas no sistema.
 */
@Slf4j
@Service
public class CambioCacheService {

    private final CambioFacade facade;
    private final long ttlMinutos;
    private volatile CambioResultado cacheado;

    public CambioCacheService(CambioFacade facade, @Value("${cambio.cache-ttl-minutos}") long ttlMinutos) {
        this.facade = facade;
        this.ttlMinutos = ttlMinutos;
    }

    public synchronized CambioObtido obterTaxaAtual() {
        if (dentroDoTtl()) {
            return new CambioObtido(cacheado, false); // AC-491
        }
        try {
            CambioResultado fresca = facade.buscarTaxaUsdBrl(); // AC-492
            cacheado = fresca;
            return new CambioObtido(fresca, false);
        } catch (IntegracaoExternaException e) {
            if (cacheado == null) {
                throw e; // AC-490: nunca teve taxa salva, sem fallback possível
            }
            log.warn("Câmbio USD-BRL indisponível ({}). Prosseguindo com última taxa conhecida.", e.getMessage());
            return new CambioObtido(cacheado, true); // AC-489
        }
    }

    private boolean dentroDoTtl() {
        return cacheado != null
                && cacheado.dataHora().isAfter(LocalDateTime.now().minusMinutes(ttlMinutos));
    }

    public record CambioObtido(CambioResultado resultado, boolean desatualizado) {
    }
}