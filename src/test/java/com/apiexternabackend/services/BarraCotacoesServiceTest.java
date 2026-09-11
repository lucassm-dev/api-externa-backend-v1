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
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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

    private BrapiResponseDTO respostaCom(BrapiResultDTO... results) {
        BrapiResponseDTO resposta = new BrapiResponseDTO();
        resposta.setResults(List.of(results));
        return resposta;
    }

    private AwesomeApiCotacaoDTO cotacaoCambio(String nome, String ask, String pctChange) {
        AwesomeApiCotacaoDTO dto = new AwesomeApiCotacaoDTO();
        dto.setName(nome);
        dto.setAsk(ask);
        dto.setPctChange(pctChange);
        return dto;
    }

    private void stubCambioECripto() {
        when(awesomeApiClient.buscarUltimas(anyString())).thenReturn(Map.of(
                "USDBRL", cotacaoCambio("Dólar", "5.10", "-0.61"),
                "EURBRL", cotacaoCambio("Euro", "5.92", "-0.34")));

        when(coinGeckoClient.buscarPreco(anyString(), anyString(), anyBoolean()))
                .thenReturn(Map.of("bitcoin", Map.of("brl", new BigDecimal("415490.12"), "brl_24h_change", new BigDecimal("5.23"))));
    }

    /**
     * Um stub por ticker, nunca um agrupado: o plano gratuito da brapi recusa chamada
     * com mais de um ativo (SPEC-12). O mock antigo devolvia dois ativos numa resposta
     * só, e foi isso que escondeu o defeito até a verificação contra a API real.
     */
    private void stubTudoOk() {
        when(brapiClient.buscarCotacao(eq("^BVSP"), any()))
                .thenReturn(respostaCom(resultado("^BVSP", null, "185188.12", "-0.01")));
        when(brapiClient.buscarCotacao(eq("PETR4"), any()))
                .thenReturn(respostaCom(resultado("PETR4", "Petroleo Brasileiro SA Pfd", "48.57", "0.42")));
        when(brapiClient.buscarCotacao(eq("VALE3"), any()))
                .thenReturn(respostaCom(resultado("VALE3", "Vale S.A.", "78.83", "-0.24")));
        stubCambioECripto();
    }

    @Test
    @DisplayName("@spec:AC-498 Barra combina itens de brapi, AwesomeAPI e CoinGecko")
    void deveCombinarItensDasTresFontes() {
        stubTudoOk();

        BarraCotacoesResponseDTO resposta = service(30).obter();

        assertThat(resposta.getItens()).extracting("simbolo")
                .containsExactlyInAnyOrder("IBOV", "PETR4", "VALE3", "USD", "EUR", "BTC");
        assertThat(resposta.getAvisos()).isEmpty();
    }

    @Test
    @DisplayName("@spec:AC-517 Barra mostra o índice IBOV e as ações PETR4 e VALE3")
    void deveMostrarIndicesDeMercado() {
        stubTudoOk();

        BarraCotacoesResponseDTO resposta = service(30).obter();

        assertThat(resposta.getItens()).extracting("simbolo").contains("IBOV", "PETR4", "VALE3");
    }

    @Test
    @DisplayName("@spec:AC-518 Cada ativo é pedido numa requisição própria, nunca agrupado")
    void devePedirUmAtivoPorRequisicao() {
        stubTudoOk();

        service(30).obter();

        verify(brapiClient).buscarCotacao(eq("^BVSP"), any());
        verify(brapiClient).buscarCotacao(eq("PETR4"), any());
        verify(brapiClient).buscarCotacao(eq("VALE3"), any());
        verify(brapiClient, never()).buscarCotacao(contains(","), any());
    }

    @Test
    @DisplayName("@spec:AC-519 Ativo que falha na brapi não impede o outro de aparecer")
    void deveIsolarFalhaDeUmAtivo() {
        when(brapiClient.buscarCotacao(eq("^BVSP"), any()))
                .thenThrow(new RuntimeException("400 QUOTES_PER_REQUEST_EXCEEDED"));
        when(brapiClient.buscarCotacao(eq("PETR4"), any()))
                .thenReturn(respostaCom(resultado("PETR4", "Petroleo Brasileiro SA Pfd", "48.57", "0.42")));
        when(brapiClient.buscarCotacao(eq("VALE3"), any()))
                .thenReturn(respostaCom(resultado("VALE3", "Vale S.A.", "78.83", "-0.24")));
        stubCambioECripto();

        BarraCotacoesResponseDTO resposta = service(30).obter();

        assertThat(resposta.getItens()).extracting("simbolo").contains("PETR4", "USD", "BTC");
        assertThat(resposta.getAvisos()).anyMatch(a -> a.contains("IBOV"));
        assertThat(resposta.getAvisos()).noneMatch(a -> a.contains("PETR4"));
    }

    @Test
    @DisplayName("@spec:AC-499 Falha na brapi não impede câmbio e cripto de aparecerem")
    void deveIsolarFalhaDeUmaFonte() {
        when(brapiClient.buscarCotacao(anyString(), any()))
                .thenThrow(new RuntimeException("brapi fora do ar"));
        when(awesomeApiClient.buscarUltimas(anyString())).thenReturn(Map.of(
                "USDBRL", cotacaoCambio("Dólar", "5.10", "-0.61")));
        when(coinGeckoClient.buscarPreco(anyString(), anyString(), anyBoolean()))
                .thenReturn(Map.of("bitcoin", Map.of("brl", new BigDecimal("415490.12"), "brl_24h_change", new BigDecimal("5.23"))));

        BarraCotacoesResponseDTO resposta = service(30).obter();

        assertThat(resposta.getItens()).extracting("simbolo").containsExactlyInAnyOrder("USD", "BTC");
        assertThat(resposta.getAvisos()).anyMatch(a -> a.contains("brapi"));
    }

    @Test
    @DisplayName("@spec:AC-500 Barra dentro do TTL reaproveita o cache sem chamar as fontes de novo")
    void deveReaproveitarCacheDentroDoTtl() {
        stubTudoOk();
        BarraCotacoesService service = service(30);

        service.obter();
        service.obter();

        verify(brapiClient, times(3)).buscarCotacao(anyString(), any()); // 3 tickers, 1 requisição cada
        verify(awesomeApiClient, times(1)).buscarUltimas(anyString());
        verify(coinGeckoClient, times(1)).buscarPreco(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("@spec:AC-520 Barra guarda o resultado por trinta minutos sem rebuscar")
    void deveGuardarResultadoPorTrintaMinutos() {
        stubTudoOk();
        BarraCotacoesService service = service(30);

        service.obter();
        service.obter();
        service.obter();

        verify(brapiClient, times(1)).buscarCotacao(eq("^BVSP"), any());
        verify(brapiClient, times(1)).buscarCotacao(eq("PETR4"), any());
        verify(brapiClient, times(1)).buscarCotacao(eq("VALE3"), any());
    }

    @Test
    @DisplayName("@spec:AC-501 Barra fora do TTL busca de novo nas três fontes")
    void deveBuscarDeNovoForaDoTtl() {
        stubTudoOk();
        BarraCotacoesService service = service(0); // TTL zero — nunca dentro do TTL

        service.obter();
        service.obter();

        verify(brapiClient, times(6)).buscarCotacao(anyString(), any()); // 3 tickers × 2 buscas
    }

    @Test
    @DisplayName("@spec:AC-502 Resposta inclui o horário em que os dados foram obtidos")
    void deveIncluirHorarioDaAtualizacao() {
        stubTudoOk();

        BarraCotacoesResponseDTO resposta = service(30).obter();

        assertThat(resposta.getAtualizadoEm()).isNotNull();
    }
}
