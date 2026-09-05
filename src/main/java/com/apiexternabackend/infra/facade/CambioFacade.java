package com.apiexternabackend.infra.facade;

import com.apiexternabackend.infra.client.awesomeapi.AwesomeApiCambioClient;
import com.apiexternabackend.infra.client.awesomeapi.dtos.AwesomeApiCotacaoDTO;
import com.apiexternabackend.infra.client.bcb.BcbPtaxClient;
import com.apiexternabackend.infra.client.bcb.dtos.BcbPtaxResponseDTO;
import com.apiexternabackend.resources.exceptions.IntegracaoExternaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Câmbio USD-BRL: AwesomeAPI é a fonte primária (campo "ask"), PTAX do Banco
 * Central (Olinda) é o fallback quando ela falha. Se as duas falharem, quem
 * chama decide o que fazer (CambioCacheService cai para a última taxa em cache).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CambioFacade {

    private final AwesomeApiCambioClient awesomeApiClient;
    private final BcbPtaxClient bcbPtaxClient;

    public CambioResultado buscarTaxaUsdBrl() {
        try {
            return buscarNaAwesomeApi(); // AC-486
        } catch (Exception e) {
            log.warn("AwesomeAPI indisponível para câmbio USD-BRL ({}). Tentando PTAX BCB.", e.getMessage());
            try {
                return buscarNoPtax(); // AC-488
            } catch (Exception e2) {
                throw new IntegracaoExternaException("EXT-011",
                        "Fontes de câmbio USD-BRL indisponíveis (AwesomeAPI e PTAX BCB).", false);
            }
        }
    }

    private CambioResultado buscarNaAwesomeApi() {
        Map<String, AwesomeApiCotacaoDTO> resposta = awesomeApiClient.buscarUsdBrl();
        if (resposta == null || resposta.isEmpty()) {
            throw new IntegracaoExternaException("EXT-011", "AwesomeAPI retornou resposta vazia para USD-BRL", false);
        }
        AwesomeApiCotacaoDTO cotacao = resposta.values().iterator().next();
        BigDecimal taxa = new BigDecimal(cotacao.getAsk());
        LocalDateTime dataHora = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(Long.parseLong(cotacao.getTimestamp())), ZoneId.systemDefault());
        return new CambioResultado(taxa, dataHora);
    }

    private CambioResultado buscarNoPtax() {
        String hoje = DateTimeFormatter.ofPattern("MM-dd-yyyy").format(LocalDate.now());
        BcbPtaxResponseDTO resposta = bcbPtaxClient.buscarCotacaoDoDia("'" + hoje + "'", "json");
        if (resposta.getValue() == null || resposta.getValue().isEmpty()) {
            throw new IntegracaoExternaException("EXT-011",
                    "PTAX BCB sem cotação para hoje (fim de semana/feriado ou fonte indisponível)", false);
        }
        BcbPtaxResponseDTO.Cotacao cotacao = resposta.getValue().get(0);
        return new CambioResultado(cotacao.getCotacaoVenda(), LocalDateTime.parse(cotacao.getDataHoraCotacao()));
    }
}