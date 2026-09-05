package com.apiexternabackend.services;

import com.apiexternabackend.domains.Acao;
import com.apiexternabackend.domains.Operacao;
import com.apiexternabackend.domains.dtos.LucroRealizadoResponseDTO;
import com.apiexternabackend.domains.enums.Mercado;
import com.apiexternabackend.domains.enums.TipoOperacao;
import com.apiexternabackend.mappers.CarteiraAcaoMapper;
import com.apiexternabackend.mappers.OperacaoMapper;
import com.apiexternabackend.repositories.CarteiraAcaoRepository;
import com.apiexternabackend.repositories.OperacaoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultaOperacaoServiceTest {

    @Mock private OperacaoRepository operacaoRepository;
    @Mock private CarteiraAcaoRepository carteiraAcaoRepository;
    @Mock private OperacaoMapper operacaoMapper;
    @Mock private CarteiraAcaoMapper carteiraAcaoMapper;
    @Mock private CarteiraService carteiraService;

    @InjectMocks
    private ConsultaOperacaoService service;

    private Operacao vendaCom(String ticker, String lucro) {
        Acao acao = new Acao(1L, ticker, ticker, Mercado.BR, "BRL", null, null, true);
        Operacao op = new Operacao();
        op.setAcao(acao);
        op.setTipo(TipoOperacao.VENDA);
        op.setLucroRealizado(new BigDecimal(lucro));
        op.setLucroRealizadoBrl(new BigDecimal(lucro)); // ação BRL: taxa 1, lucro em BRL == lucro nativo
        op.setDataHora(LocalDateTime.now());
        return op;
    }

    @Test
    @DisplayName("@spec:AC-470 Lucro realizado total soma apenas vendas ativas da carteira")
    void deveSomarLucroRealizadoTotalDaCarteira() {
        when(operacaoRepository.findByCarteiraIdAndTipoAndAtivoTrue(1L, TipoOperacao.VENDA))
                .thenReturn(List.of(vendaCom("PETR4", "100"), vendaCom("VALE3", "50")));

        LucroRealizadoResponseDTO resposta = service.lucroRealizado(1L, 10L);

        assertThat(resposta.getTotal()).isEqualByComparingTo("150");
    }

    @Test
    @DisplayName("@spec:AC-471 Lucro realizado é agrupado por ticker")
    void deveAgruparLucroRealizadoPorTicker() {
        when(operacaoRepository.findByCarteiraIdAndTipoAndAtivoTrue(1L, TipoOperacao.VENDA))
                .thenReturn(List.of(vendaCom("PETR4", "100"), vendaCom("PETR4", "20"), vendaCom("VALE3", "50")));

        LucroRealizadoResponseDTO resposta = service.lucroRealizado(1L, 10L);

        assertThat(resposta.getPorTicker().get("PETR4")).isEqualByComparingTo("120");
        assertThat(resposta.getPorTicker().get("VALE3")).isEqualByComparingTo("50");
    }

    @Test
    @DisplayName("@spec:AC-496 Soma vendas de ações BRL e USD já convertidas, não os valores brutos")
    void deveSomarLucroRealizadoConvertidoEmCarteiraMultiMoeda() {
        Operacao vendaBrl = vendaCom("PETR4", "100"); // BRL: lucroRealizadoBrl == lucroRealizado
        Operacao vendaUsd = vendaCom("AAPL", "10"); // lucroRealizado nativo (USD) = 10
        vendaUsd.setLucroRealizadoBrl(new BigDecimal("50")); // convertido (taxa 5, por exemplo)
        when(operacaoRepository.findByCarteiraIdAndTipoAndAtivoTrue(1L, TipoOperacao.VENDA))
                .thenReturn(List.of(vendaBrl, vendaUsd));

        LucroRealizadoResponseDTO resposta = service.lucroRealizado(1L, 10L);

        // 100 (BRL) + 50 (USD convertido) = 150 — nunca 100 + 10 (misturaria moedas)
        assertThat(resposta.getTotal()).isEqualByComparingTo("150");
    }
}
