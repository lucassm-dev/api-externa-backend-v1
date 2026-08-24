package com.apiexternabackend.infra.adapter;

import com.apiexternabackend.infra.client.brapi.BrapiClient;
import com.apiexternabackend.infra.client.brapi.dtos.BrapiResponseDTO;
import com.apiexternabackend.infra.client.brapi.dtos.BrapiResultDTO;
import com.apiexternabackend.resources.exceptions.BusinessException;
import com.apiexternabackend.resources.exceptions.ExternalServiceException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BrapiAdapter implements CotacaoAdapter {

    private final BrapiClient client;

    @Value("${BRAPI_TOKEN:}")
    private String token;

    @Override
    public CotacaoResultado buscarCotacao(String ticker) {
        try {
            BrapiResponseDTO response = client.buscarCotacao(ticker, token.isBlank() ? null : token);
            List<BrapiResultDTO> results = response.getResults();

            if (results == null || results.isEmpty()) {
                throw new BusinessException("Ticker não encontrado na fonte BR: " + ticker);
            }

            BrapiResultDTO result = results.get(0);

            if (result.getRegularMarketPrice() == null) {
                throw new BusinessException("Ticker não encontrado na fonte BR: " + ticker);
            }

            LocalDateTime dataHora = result.getRegularMarketTime() != null
                    ? LocalDateTime.ofInstant(Instant.ofEpochSecond(result.getRegularMarketTime()), ZoneId.systemDefault())
                    : LocalDateTime.now();

            return new CotacaoResultado(result.getRegularMarketPrice(), dataHora);
        } catch (BusinessException e) {
            throw e;
        } catch (FeignException.NotFound e) {
            throw new BusinessException("Ticker não encontrado na fonte BR: " + ticker);
        } catch (FeignException e) {
            if (e.status() == 429) {
                throw new ExternalServiceException("Limite de requisições da fonte BR excedido. Tente mais tarde.");
            }
            throw new ExternalServiceException("Fonte BR (brapi) indisponível: " + e.getMessage(), e);
        }
    }
}
