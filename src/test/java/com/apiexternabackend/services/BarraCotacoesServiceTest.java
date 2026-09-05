package com.apiexternabackend.services;

import com.apiexternabackend.domains.dtos.BarraCotacoesResponseDTO;
import com.apiexternabackend.infra.client.awesomeapi.AwesomeApiCambioClient;
import com.apiexternabackend.infra.client.awesomeapi.dtos.AwesomeApiCotacaoDTO;
import com.apiexternabackend.infra.client.brapi.BrapiClient;
import com.apiexternabackend.infra.client.brapi.dtos.BrapiResponseDTO;
import com.apiexternabackend.infra.client.brapi.dtos.BrapiResultDTO;
import com.apiexternabackend.infra.client.coingecko.CoinGeckoClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BarraCotacoesServiceTest {

    @Mock private BrapiClient brapiClient;
    @Mock private AwesomeApiCambioClient awesomeApiClient;
    @Mock private CoinGeckoClient coinGeckoClient;

    private BarraCotacoesService service(long ttlMinutos) {
        return new BarraCotacoesService(brapiClient, awesomeApiClient, coinGeckoClient, "", ttlMinutos);
    }

    private BrapiResultDTO resultado(String symbol, String shortName, String preco, String variacao) {
        BrapiResultDTO r = new BrapiResultDTO();
        r.setSymbol(symbol);
        r.setShortName(shortName);
        r.setRegularMarketPrice(new BigDecimal(preco));
        r.setRegularMarketChangePercent(new BigDecimal(variacao));
        return r;
    }

    private AwesomeApiCotacaoDTO cotacaoCambio(String nome, String ask, String pctChange) {
        AwesomeApiCotacaoDTO dto = new AwesomeApiCotacaoDTO();
        dto.setName(nome);
        dto.setAsk(ask);
        dto.setPctChange(pctChange);
        return dto;
    }

    private void stubTudoOk() {
        BrapiResponseDTO resposta = new BrapiResponseDTO();
        resposta.setResults(List.of(
                resultado("PETR4", "Petrobras PN", "47.50", "-1.31"),
                resultado("^BVSP", null, "185188.12", "-0.01")));
        when(brapiClient.buscarCotacao(anyString(), any())).thenReturn(resposta);

        when(awesomeApiClient.buscarUltimas(anyString())).thenReturn(Map.of(
                "USDBRL", cotacaoCambio("Dólar", "5.10", "-0.61"),
                "EURBRL", cotacaoCambio("Euro", "5.92", "-0.34")));

        when(coinGeckoClient.buscarPreco(anyString(), anyString(), anyBoolean()))
                .thenReturn(Map.of("bitcoin", Map.of("brl", new BigDecimal("415490.12"), "brl_24h_change", new BigDecimal("5.23"))));
    }

    @Test
    @DisplayName("@spec:AC-498 Barra combina itens de brapi, AwesomeAPI e CoinGecko")
    void deveCombinarItensDasTresFontes() {
        stubTudoOk();

        BarraCotacoesResponseDTO resposta = service(15).obter();

        assertThat(resposta.getItens()).extracting("simbolo")
                .containsExactlyInAnyOrder("PETR4", "IBOV", "USD", "EUR", "BTC");
        assertThat(resposta.getAvisos()).isEmpty();
    }

    @Test
    @DisplayName("@spec:AC-499 Falha na brapi não impede câmbio e cripto de aparecerem")
    void deveIsolarFalhaDeUmaFonte() {
        when(brapiClient.buscarCotacao(anyString(), any())).thenThrow(new RuntimeException("brapi fora do ar"));
        when(awesomeApiClient.buscarUltimas(anyString())).thenReturn(Map.of(
                "USDBRL", cotacaoCambio("Dólar", "5.10", "-0.61")));
        when(coinGeckoClient.buscarPreco(anyString(), anyString(), anyBoolean()))
                .thenReturn(Map.of("bitcoin", Map.of("brl", new BigDecimal("415490.12"), "brl_24h_change", new BigDecimal("5.23"))));

        BarraCotacoesResponseDTO resposta = service(15).obter();

        assertThat(resposta.getItens()).extracting("simbolo").containsExactlyInAnyOrder("USD", "BTC");
        assertThat(resposta.getAvisos()).anyMatch(a -> a.contains("brapi"));
    }

    @Test
    @DisplayName("@spec:AC-500 Barra dentro do TTL reaproveita o cache sem chamar as fontes de novo")
    void deveReaproveitarCacheDentroDoTtl() {
        stubTudoOk();
        BarraCotacoesService service = service(15);

        service.obter();
        service.obter();

        verify(brapiClient, times(1)).buscarCotacao(anyString(), any());
        verify(awesomeApiClient, times(1)).buscarUltimas(anyString());
        verify(coinGeckoClient, times(1)).buscarPreco(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("@spec:AC-501 Barra fora do TTL busca de novo nas três fontes")
    void deveBuscarDeNovoForaDoTtl() {
        stubTudoOk();
        BarraCotacoesService service = service(0); // TTL zero — nunca dentro do TTL

        service.obter();
        service.obter();

        verify(brapiClient, times(2)).buscarCotacao(anyString(), any());
    }

    @Test
    @DisplayName("@spec:AC-502 Resposta inclui o horário em que os dados foram obtidos")
    void deveIncluirHorarioDaAtualizacao() {
        stubTudoOk();

        BarraCotacoesResponseDTO resposta = service(15).obter();

        assertThat(resposta.getAtualizadoEm()).isNotNull();
    }
}
