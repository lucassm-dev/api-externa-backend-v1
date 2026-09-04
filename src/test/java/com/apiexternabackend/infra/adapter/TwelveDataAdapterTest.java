package com.apiexternabackend.infra.adapter;

import com.apiexternabackend.infra.client.twelvedata.TwelveDataClient;
import com.apiexternabackend.infra.client.twelvedata.dtos.TwelveDataResponseDTO;
import com.apiexternabackend.resources.exceptions.IntegracaoExternaException;
import com.apiexternabackend.resources.exceptions.RegraVioladaException;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TwelveDataAdapterTest {

    @Mock
    private TwelveDataClient client;

    @InjectMocks
    private TwelveDataAdapter adapter;

    private FeignException feignExceptionComStatus(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "/price",
                Collections.emptyMap(), null, new RequestTemplate());
        Response response = Response.builder().status(status).request(request).build();
        return FeignException.errorStatus("TwelveDataClient#buscarCotacao", response);
    }

    @Test
    @DisplayName("@spec:AC-429 Twelve Data responde erro de cota (status=error, mensagem de rate limit) -> IntegracaoExternaException EXT-009 mapeando para 429")
    void limiteDeRequisicoesExcedidoMapeiaParaExt009() {
        ReflectionTestUtils.setField(adapter, "apiKey", "chave");
        TwelveDataResponseDTO response = new TwelveDataResponseDTO();
        response.setStatus("error");
        response.setMessage("You have reached the rate limit for the current minute.");
        when(client.buscarCotacao(anyString(), anyString())).thenReturn(response);

        assertThatThrownBy(() -> adapter.buscarCotacao("AAPL"))
                .isInstanceOf(IntegracaoExternaException.class)
                .satisfies(ex -> {
                    IntegracaoExternaException e = (IntegracaoExternaException) ex;
                    assertThat(e.getCodigo()).isEqualTo("EXT-009");
                    assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(e.isLimiteExcedido()).isTrue();
                });
    }

    @Test
    @DisplayName("@spec:AC-430 Twelve Data indisponível (5xx) -> IntegracaoExternaException EXT-010 mapeando para 503, nunca 500")
    void fonteIndisponivelMapeiaParaExt010() {
        ReflectionTestUtils.setField(adapter, "apiKey", "chave");
        when(client.buscarCotacao(anyString(), anyString())).thenThrow(feignExceptionComStatus(502));

        assertThatThrownBy(() -> adapter.buscarCotacao("AAPL"))
                .isInstanceOf(IntegracaoExternaException.class)
                .satisfies(ex -> {
                    IntegracaoExternaException e = (IntegracaoExternaException) ex;
                    assertThat(e.getCodigo()).isEqualTo("EXT-010");
                    assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(e.isLimiteExcedido()).isFalse();
                });
    }

    @Test
    @DisplayName("Ticker não encontrado na Twelve Data (status=error, sem rate limit) continua RegraVioladaException EXT-008")
    void tickerNaoEncontradoContinuaRegraVioladaExt008() {
        ReflectionTestUtils.setField(adapter, "apiKey", "chave");
        TwelveDataResponseDTO response = new TwelveDataResponseDTO();
        response.setStatus("error");
        response.setMessage("**symbol** not found: INVALIDO");
        when(client.buscarCotacao(anyString(), anyString())).thenReturn(response);

        assertThatThrownBy(() -> adapter.buscarCotacao("INVALIDO"))
                .isInstanceOf(RegraVioladaException.class)
                .satisfies(ex -> assertThat(((RegraVioladaException) ex).getCodigo()).isEqualTo("EXT-008"));
    }

    @Test
    @DisplayName("Cotação encontrada com sucesso retorna CotacaoResultado")
    void cotacaoEncontradaComSucesso() {
        ReflectionTestUtils.setField(adapter, "apiKey", "chave");
        TwelveDataResponseDTO response = new TwelveDataResponseDTO();
        response.setPrice("172.35");
        when(client.buscarCotacao(anyString(), anyString())).thenReturn(response);

        CotacaoResultado cotacao = adapter.buscarCotacao("AAPL");

        assertThat(cotacao.preco()).isEqualByComparingTo("172.35");
    }
}
