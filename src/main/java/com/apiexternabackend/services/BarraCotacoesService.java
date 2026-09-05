package com.apiexternabackend.services;

import com.apiexternabackend.domains.dtos.BarraCotacoesResponseDTO;
import com.apiexternabackend.domains.dtos.ItemBarraCotacoesDTO;
import com.apiexternabackend.infra.client.awesomeapi.AwesomeApiCambioClient;
import com.apiexternabackend.infra.client.awesomeapi.dtos.AwesomeApiCotacaoDTO;
import com.apiexternabackend.infra.client.brapi.BrapiClient;
import com.apiexternabackend.infra.client.brapi.dtos.BrapiResultDTO;
import com.apiexternabackend.infra.client.coingecko.CoinGeckoClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Barra de cotações (Bloco B.2): agrega ações/índices (brapi), câmbio
 * (AwesomeAPI) e cripto (CoinGecko) numa única lista, com cache com TTL.
 * Diferente do CambioFacade (SPEC-08), aqui as três fontes são complementares
 * — cada uma dona de um subconjunto de itens — não uma cadeia de fallback:
 * a falha de uma não deve impedir as outras de aparecer (AC-499).
 */
@Slf4j
@Service
public class BarraCotacoesService {

    private static final String TICKERS_BRAPI = "PETR4,ITUB4,IVVB11,^BVSP,IFIX.SA";
    private static final String PARES_CAMBIO = "USD-BRL,EUR-BRL";

    private final BrapiClient brapiClient;
    private final AwesomeApiCambioClient awesomeApiClient;
    private final CoinGeckoClient coinGeckoClient;
    private final String brapiToken;
    private final long ttlMinutos;

    private volatile BarraCotacoesResponseDTO cacheada;

    public BarraCotacoesService(BrapiClient brapiClient, AwesomeApiCambioClient awesomeApiClient,
                                 CoinGeckoClient coinGeckoClient,
                                 @Value("${BRAPI_TOKEN:}") String brapiToken,
                                 @Value("${mercado.barra-cache-ttl-minutos}") long ttlMinutos) {
        this.brapiClient = brapiClient;
        this.awesomeApiClient = awesomeApiClient;
        this.coinGeckoClient = coinGeckoClient;
        this.brapiToken = brapiToken;
        this.ttlMinutos = ttlMinutos;
    }

    public synchronized BarraCotacoesResponseDTO obter() {
        if (dentroDoTtl()) {
            return cacheada; // AC-500
        }

        List<ItemBarraCotacoesDTO> itens = new ArrayList<>();
        List<String> avisos = new ArrayList<>();

        try {
            itens.addAll(buscarAcoesEIndicesNaBrapi());
        } catch (Exception e) {
            log.warn("Ações/índices indisponíveis na brapi para a barra de cotações: {}", e.getMessage());
            avisos.add("Ações/índices indisponíveis no momento (brapi).");
        }

        try {
            itens.addAll(buscarCambioNaAwesomeApi());
        } catch (Exception e) {
            log.warn("Câmbio indisponível na AwesomeAPI para a barra de cotações: {}", e.getMessage());
            avisos.add("Câmbio indisponível no momento (AwesomeAPI).");
        }

        try {
            itens.add(buscarBtcNoCoinGecko());
        } catch (Exception e) {
            log.warn("Cripto indisponível na CoinGecko para a barra de cotações: {}", e.getMessage());
            avisos.add("Cripto indisponível no momento (CoinGecko).");
        }

        cacheada = new BarraCotacoesResponseDTO(itens, LocalDateTime.now(), avisos); // AC-501/AC-502
        return cacheada;
    }

    private boolean dentroDoTtl() {
        return cacheada != null
                && cacheada.getAtualizadoEm().isAfter(LocalDateTime.now().minusMinutes(ttlMinutos));
    }

    private List<ItemBarraCotacoesDTO> buscarAcoesEIndicesNaBrapi() { // AC-498
        List<BrapiResultDTO> results = brapiClient
                .buscarCotacao(TICKERS_BRAPI, brapiToken.isBlank() ? null : brapiToken)
                .getResults();

        List<ItemBarraCotacoesDTO> itens = new ArrayList<>();
        if (results == null) {
            return itens;
        }
        for (BrapiResultDTO r : results) {
            String nome = r.getShortName() != null ? r.getShortName()
                    : r.getLongName() != null ? r.getLongName() : r.getSymbol();
            itens.add(new ItemBarraCotacoesDTO(
                    simboloExibicao(r.getSymbol()), nome, r.getRegularMarketPrice(),
                    r.getRegularMarketChangePercent(), r.getLogourl()));
        }
        return itens;
    }

    private String simboloExibicao(String symbolBrapi) {
        if ("^BVSP".equals(symbolBrapi)) {
            return "IBOV";
        }
        if ("IFIX.SA".equals(symbolBrapi)) {
            return "IFIX";
        }
        return symbolBrapi;
    }

    private List<ItemBarraCotacoesDTO> buscarCambioNaAwesomeApi() { // AC-498
        Map<String, AwesomeApiCotacaoDTO> resposta = awesomeApiClient.buscarUltimas(PARES_CAMBIO);
        List<ItemBarraCotacoesDTO> itens = new ArrayList<>();
        for (Map.Entry<String, AwesomeApiCotacaoDTO> entrada : resposta.entrySet()) {
            AwesomeApiCotacaoDTO cot = entrada.getValue();
            String simbolo = entrada.getKey().replace("BRL", "");
            itens.add(new ItemBarraCotacoesDTO(
                    simbolo, cot.getName(), new BigDecimal(cot.getAsk()),
                    cot.getPctChange() != null ? new BigDecimal(cot.getPctChange()) : null, null));
        }
        return itens;
    }

    private ItemBarraCotacoesDTO buscarBtcNoCoinGecko() { // AC-498
        Map<String, Map<String, BigDecimal>> resposta =
                coinGeckoClient.buscarPreco("bitcoin", "brl", true);
        Map<String, BigDecimal> btc = resposta.get("bitcoin");
        return new ItemBarraCotacoesDTO("BTC", "Bitcoin", btc.get("brl"), btc.get("brl_24h_change"), null);
    }
}