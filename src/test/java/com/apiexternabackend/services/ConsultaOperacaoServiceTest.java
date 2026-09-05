package com.apiexternabackend.services;

import com.apiexternabackend.domains.Acao;
import com.apiexternabackend.domains.CarteiraAcao;
import com.apiexternabackend.domains.Operacao;
import com.apiexternabackend.domains.dtos.CarteiraConsolidadaResponseDTO;
import com.apiexternabackend.domains.dtos.LucroRealizadoResponseDTO;
import com.apiexternabackend.domains.enums.Mercado;
import com.apiexternabackend.domains.enums.TipoOperacao;
import com.apiexternabackend.infra.facade.CambioResultado;
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
    @Mock private CambioCacheService cambioCacheService;

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

    private CarteiraAcao posicao(String moeda, int quantidade, BigDecimal cotacaoAtual, BigDecimal custoTotalBrl) {
        Acao acao = new Acao(1L, moeda.equals("USD") ? "AAPL" : "PETR4", "Empresa",
                moeda.equals("USD") ? Mercado.US : Mercado.BR, moeda, cotacaoAtual, LocalDateTime.now(), true);
        CarteiraAcao pos = new CarteiraAcao();
        pos.setAcao(acao);
        pos.setQuantidade(quantidade);
        pos.setCustoTotalBrl(custoTotalBrl);
        return pos;
    }

    @Test
    @DisplayName("@spec:AC-493 @spec:AC-495 Consolidado soma valor investido e calcula valor de mercado com câmbio atual (BRL = taxa 1)")
    void deveCalcularConsolidadoComCarteiraMultiMoeda() {
        CarteiraAcao posicaoBrl = posicao("BRL", 100, new BigDecimal("40"), new BigDecimal("3000")); // investido 3000, mercado 100*40=4000
        CarteiraAcao posicaoUsd = posicao("USD", 50, new BigDecimal("160"), new BigDecimal("10000")); // investido 10000 BRL, mercado 50*160*taxa
        when(carteiraAcaoRepository.findByCarteiraId(1L)).thenReturn(List.of(posicaoBrl, posicaoUsd));
        when(cambioCacheService.obterTaxaAtual()).thenReturn(
                new CambioCacheService.CambioObtido(new CambioResultado(new BigDecimal("5"), LocalDateTime.now()), false));

        CarteiraConsolidadaResponseDTO resposta = service.consolidado(1L, 10L);

        // investido = 3000 + 10000 = 13000
        assertThat(resposta.getValorInvestido()).isEqualByComparingTo("13000");
        // mercado = (100*40*1) + (50*160*5) = 4000 + 40000 = 44000
        assertThat(resposta.getValorDeMercado()).isEqualByComparingTo("44000");
        assertThat(resposta.getLucroNaoRealizado()).isEqualByComparingTo("31000");
    }

    @Test
    @DisplayName("@spec:AC-494 Consolidado exibe a taxa de câmbio atual usada e o horário")
    void deveExibirTaxaEHorarioNoConsolidado() {
        CarteiraAcao posicaoUsd = posicao("USD", 10, new BigDecimal("100"), new BigDecimal("1000"));
        when(carteiraAcaoRepository.findByCarteiraId(1L)).thenReturn(List.of(posicaoUsd));
        LocalDateTime agora = LocalDateTime.now();
        when(cambioCacheService.obterTaxaAtual()).thenReturn(
                new CambioCacheService.CambioObtido(new CambioResultado(new BigDecimal("5.25"), agora), false));

        CarteiraConsolidadaResponseDTO resposta = service.consolidado(1L, 10L);

        assertThat(resposta.getTaxaCambioAtual()).isEqualByComparingTo("5.25");
        assertThat(resposta.getDataHoraTaxaCambio()).isEqualTo(agora);
    }

    @Test
    @DisplayName("Consolidado de carteira 100% BRL não chama a fonte de câmbio")
    void naoDeveChamarCambioParaCarteira100PorCentoBrl() {
        CarteiraAcao posicaoBrl = posicao("BRL", 100, new BigDecimal("40"), new BigDecimal("3000"));
        when(carteiraAcaoRepository.findByCarteiraId(1L)).thenReturn(List.of(posicaoBrl));

        CarteiraConsolidadaResponseDTO resposta = service.consolidado(1L, 10L);

        assertThat(resposta.getTaxaCambioAtual()).isEqualByComparingTo("1");
        org.mockito.Mockito.verify(cambioCacheService, org.mockito.Mockito.never()).obterTaxaAtual();
    }
}
