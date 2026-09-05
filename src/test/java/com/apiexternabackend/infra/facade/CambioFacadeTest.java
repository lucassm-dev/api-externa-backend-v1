package com.apiexternabackend.infra.facade;

import com.apiexternabackend.infra.client.awesomeapi.AwesomeApiCambioClient;
import com.apiexternabackend.infra.client.awesomeapi.dtos.AwesomeApiCotacaoDTO;
import com.apiexternabackend.infra.client.bcb.BcbPtaxClient;
import com.apiexternabackend.infra.client.bcb.dtos.BcbPtaxResponseDTO;
import com.apiexternabackend.resources.exceptions.IntegracaoExternaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CambioFacadeTest {

    @Mock private AwesomeApiCambioClient awesomeApiClient;
    @Mock private BcbPtaxClient bcbPtaxClient;

    @InjectMocks
    private CambioFacade facade;

    private AwesomeApiCotacaoDTO cotacaoAwesome(String ask) {
        AwesomeApiCotacaoDTO dto = new AwesomeApiCotacaoDTO();
        dto.setAsk(ask);
        dto.setTimestamp("1700000000");
        return dto;
    }

    @Test
    @DisplayName("@spec:AC-486 Busca a taxa usando o campo ask da AwesomeAPI")
    void deveBuscarTaxaNaAwesomeApi() {
        when(awesomeApiClient.buscarUsdBrl()).thenReturn(Map.of("USDBRL", cotacaoAwesome("5.31")));

        CambioResultado resultado = facade.buscarTaxaUsdBrl();

        assertThat(resultado.taxa()).isEqualByComparingTo("5.31");
    }

    @Test
    @DisplayName("@spec:AC-488 AwesomeAPI falha, tenta o fallback PTAX BCB")
    void deveCairParaPtaxQuandoAwesomeApiFalha() {
        when(awesomeApiClient.buscarUsdBrl()).thenThrow(new RuntimeException("timeout"));
        BcbPtaxResponseDTO.Cotacao cotacaoPtax = new BcbPtaxResponseDTO.Cotacao();
        cotacaoPtax.setCotacaoVenda(new BigDecimal("5.30"));
        cotacaoPtax.setDataHoraCotacao("2026-09-05T13:00:00.000");
        BcbPtaxResponseDTO respostaPtax = new BcbPtaxResponseDTO();
        respostaPtax.setValue(List.of(cotacaoPtax));
        when(bcbPtaxClient.buscarCotacaoDoDia(any(), any())).thenReturn(respostaPtax);

        CambioResultado resultado = facade.buscarTaxaUsdBrl();

        assertThat(resultado.taxa()).isEqualByComparingTo("5.30");
        verify(bcbPtaxClient).buscarCotacaoDoDia(any(), any());
    }

    @Test
    @DisplayName("Ambas as fontes falham lança IntegracaoExternaException")
    void deveLancarExcecaoQuandoAmbasFontesFalham() {
        when(awesomeApiClient.buscarUsdBrl()).thenThrow(new RuntimeException("timeout"));
        when(bcbPtaxClient.buscarCotacaoDoDia(any(), any())).thenThrow(new RuntimeException("indisponível"));

        assertThatThrownBy(() -> facade.buscarTaxaUsdBrl())
                .isInstanceOf(IntegracaoExternaException.class)
                .extracting(e -> ((IntegracaoExternaException) e).getCodigo())
                .isEqualTo("EXT-011");
    }
}
