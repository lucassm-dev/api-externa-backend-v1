package com.apiexternabackend.infra.adapter;

import com.apiexternabackend.infra.client.brapi.BrapiClient;
import com.apiexternabackend.infra.client.brapi.dtos.BrapiResponseDTO;
import com.apiexternabackend.infra.client.brapi.dtos.BrapiResultDTO;
import com.apiexternabackend.resources.exceptions.IntegracaoExternaException;
import com.apiexternabackend.resources.exceptions.RegraVioladaException;
import feign.FeignException;
import feign.Request;
import feign.Response;
import feign.RequestTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrapiAdapterTest {

    @Mock
    private BrapiClient client;

    @InjectMocks
    private BrapiAdapter adapter;

    private FeignException feignExceptionComStatus(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "/quote/PETR4",
                Collections.emptyMap(), null, new RequestTemplate());
        Response response = Response.builder().status(status).request(request).build();
        return FeignException.errorStatus("BrapiClient#buscarCotacao", response);
    }

    @Test
    @DisplayName("@spec:AC-429 Brapi retorna HTTP 429 -> IntegracaoExternaException EXT-009 mapeando para 429")
    void limiteDeRequisicoesExcedidoMapeiaParaExt009() {
        ReflectionTestUtils.setField(adapter, "token", "");
        when(client.buscarCotacao(anyString(), any())).thenThrow(feignExceptionComStatus(429));

        assertThatThrownBy(() -> adapter.buscarCotacao("PETR4"))
                .isInstanceOf(IntegracaoExternaException.class)
                .satisfies(ex -> {
                    IntegracaoExternaException e = (IntegracaoExternaException) ex;
                    assertThat(e.getCodigo()).isEqualTo("EXT-009");
                    assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(e.isLimiteExcedido()).isTrue();
                });
    }

    @Test
    @DisplayName("@spec:AC-430 Brapi indisponível (5xx) -> IntegracaoExternaException EXT-010 mapeando para 503, nunca 500")
    void fonteIndisponivelMapeiaParaExt010() {
        ReflectionTestUtils.setField(adapter, "token", "");
        when(client.buscarCotacao(anyString(), any())).thenThrow(feignExceptionComStatus(502));

        assertThatThrownBy(() -> adapter.buscarCotacao("PETR4"))
                .isInstanceOf(IntegracaoExternaException.class)
                .satisfies(ex -> {
                    IntegracaoExternaException e = (IntegracaoExternaException) ex;
                    assertThat(e.getCodigo()).isEqualTo("EXT-010");
                    assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(e.isLimiteExcedido()).isFalse();
                });
    }

    @Test
    @DisplayName("Ticker não encontrado na Brapi (404) continua RegraVioladaException EXT-008")
    void tickerNaoEncontradoContinuaRegraVioladaExt008() {
        ReflectionTestUtils.setField(adapter, "token", "");
        when(client.buscarCotacao(anyString(), any())).thenThrow(feignExceptionComStatus(404));

        assertThatThrownBy(() -> adapter.buscarCotacao("INVALIDO"))
                .isInstanceOf(RegraVioladaException.class)
                .satisfies(ex -> {
                    RegraVioladaException e = (RegraVioladaException) ex;
                    assertThat(e.getCodigo()).isEqualTo("EXT-008");
                    assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                });
    }

    @Test
    @DisplayName("Resultado vazio na Brapi continua RegraVioladaException EXT-008")
    void resultadoVazioContinuaRegraVioladaExt008() {
        ReflectionTestUtils.setField(adapter, "token", "");
        BrapiResponseDTO response = new BrapiResponseDTO();
        response.setResults(List.of());
        when(client.buscarCotacao(anyString(), any())).thenReturn(response);

        assertThatThrownBy(() -> adapter.buscarCotacao("INVALIDO"))
                .isInstanceOf(RegraVioladaException.class)
                .satisfies(ex -> assertThat(((RegraVioladaException) ex).getCodigo()).isEqualTo("EXT-008"));
    }

    @Test
    @DisplayName("Cotação encontrada com sucesso retorna CotacaoResultado")
    void cotacaoEncontradaComSucesso() {
        ReflectionTestUtils.setField(adapter, "token", "");
        BrapiResultDTO resultado = new BrapiResultDTO();
        resultado.setRegularMarketPrice(new BigDecimal("35.50"));
        BrapiResponseDTO response = new BrapiResponseDTO();
        response.setResults(List.of(resultado));
        when(client.buscarCotacao(anyString(), any())).thenReturn(response);

        CotacaoResultado cotacao = adapter.buscarCotacao("PETR4");

        assertThat(cotacao.preco()).isEqualByComparingTo("35.50");
    }
}
