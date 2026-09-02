package com.apiexternabackend.infra.adapter;

import com.apiexternabackend.infra.client.twelvedata.TwelveDataClient;
import com.apiexternabackend.infra.client.twelvedata.dtos.TwelveDataResponseDTO;
import com.apiexternabackend.resources.exceptions.BusinessException;
import com.apiexternabackend.resources.exceptions.ExternalServiceException;
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
                    throw new ExternalServiceException("Limite de requisições da fonte US excedido. Tente mais tarde.");
                }
                String msg = response.getMessage() != null ? response.getMessage() : "Ticker não encontrado";
                throw new BusinessException("Ticker não encontrado na fonte US: " + ticker + " — " + msg);
            }

            if (response.getPrice() == null || response.getPrice().isBlank()) {
                throw new BusinessException("Ticker não encontrado na fonte US: " + ticker);
            }

            BigDecimal preco = new BigDecimal(response.getPrice());
            return new CotacaoResultado(preco, LocalDateTime.now());
        } catch (BusinessException | ExternalServiceException e) {
            throw e;
        } catch (FeignException.NotFound e) {
            throw new BusinessException("Ticker não encontrado na fonte US: " + ticker);
        } catch (FeignException e) {
            if (e.status() == 429) {
                throw new ExternalServiceException("Limite de requisições da fonte US excedido. Tente mais tarde.");
            }
            throw new ExternalServiceException("Fonte US (Twelve Data) indisponível. Tente novamente mais tarde.");
        }
    }
}
