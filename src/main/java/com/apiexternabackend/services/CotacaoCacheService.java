package com.apiexternabackend.services;

import com.apiexternabackend.domains.Acao;
import com.apiexternabackend.domains.enums.Mercado;
import com.apiexternabackend.infra.adapter.BrapiAdapter;
import com.apiexternabackend.infra.adapter.CotacaoAdapter;
import com.apiexternabackend.infra.adapter.CotacaoResultado;
import com.apiexternabackend.infra.adapter.TwelveDataAdapter;
import com.apiexternabackend.repositories.AcaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Cache de cotação com TTL (Q-MAP-05): reaproveita cotacaoAtual/dataHoraCotacao
 * já persistidos em Acao em vez de bater na fonte externa a cada chamada.
 */
@Service
@RequiredArgsConstructor
public class CotacaoCacheService {

    private final AcaoRepository acaoRepository;
    private final BrapiAdapter brapiAdapter;
    private final TwelveDataAdapter twelveDataAdapter;

    @Value("${cotacao.cache-ttl-minutos}")
    private long ttlMinutos;

    /**
     * Retorna a cotação de uma ação já persistida, reaproveitando o cache quando
     * dentro do TTL. Fora do TTL (ou com forcarAtualizacao=true), busca na fonte
     * externa e persiste o novo valor — pode lançar IntegracaoExternaException.
     */
    public CotacaoResultado obter(Acao acao, boolean forcarAtualizacao) {
        if (!forcarAtualizacao && dentroDoTtl(acao)) { // AC-477
            return new CotacaoResultado(acao.getCotacaoAtual(), acao.getDataHoraCotacao());
        }

        CotacaoResultado fresca = adapterPara(acao.getMercado()).buscarCotacao(acao.getTicker()); // AC-478/AC-481
        acao.setCotacaoAtual(fresca.preco());
        acao.setDataHoraCotacao(fresca.dataHora());
        acaoRepository.save(acao);
        return fresca;
    }

    private boolean dentroDoTtl(Acao acao) {
        return acao.getDataHoraCotacao() != null
                && acao.getDataHoraCotacao().isAfter(LocalDateTime.now().minusMinutes(ttlMinutos)); // AC-480
    }

    private CotacaoAdapter adapterPara(Mercado mercado) {
        return mercado == Mercado.BR ? brapiAdapter : twelveDataAdapter;
    }
}
