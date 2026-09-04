package com.apiexternabackend.infra.adapter;

import com.apiexternabackend.infra.client.twelvedata.TwelveDataClient;
import com.apiexternabackend.infra.client.twelvedata.dtos.TwelveDataResponseDTO;
import com.apiexternabackend.resources.exceptions.IntegracaoExternaException;
import com.apiexternabackend.resources.exceptions.RegraVioladaException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class TwelveDataAdapter implements CotacaoAdapter {

    private final TwelveDataClient client;

    @Value("${TWELVEDATA_API_KEY:}")
    private String apiKey;

    @Override
    public CotacaoResultado buscarCotacao(String ticker) {
        try {
            TwelveDataResponseDTO response = client.buscarCotacao(ticker, apiKey);

            if (response.isError()) {
                if (response.isRateLimit()) {
                    throw new IntegracaoExternaException("EXT-009",
                            "Limite de requisições da fonte US excedido. Tente novamente mais tarde.", true);
                }
                String msg = response.getMessage() != null ? response.getMessage() : "Ticker não encontrado";
                throw new RegraVioladaException("EXT-008", "Ticker não encontrado na fonte US: " + ticker + " — " + msg);
            }

            if (response.getPrice() == null || response.getPrice().isBlank()) {
                throw new RegraVioladaException("EXT-008", "Ticker não encontrado na fonte US: " + ticker);
            }

            BigDecimal preco = new BigDecimal(response.getPrice());
            return new CotacaoResultado(preco, LocalDateTime.now());
        } catch (RegraVioladaException | IntegracaoExternaException e) {
            throw e;
        } catch (FeignException.NotFound e) {
            throw new RegraVioladaException("EXT-008", "Ticker não encontrado na fonte US: " + ticker);
        } catch (FeignException e) {
            if (e.status() == 429) {
                throw new IntegracaoExternaException("EXT-009",
                        "Limite de requisições da fonte US excedido. Tente novamente mais tarde.", true);
            }
            throw new IntegracaoExternaException("EXT-010",
                    "Fonte US (Twelve Data) indisponível. Tente novamente mais tarde.", false);
        }
    }
}
