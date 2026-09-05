package com.apiexternabackend.services;

import com.apiexternabackend.domains.Acao;
import com.apiexternabackend.domains.Carteira;
import com.apiexternabackend.domains.CarteiraAcao;
import com.apiexternabackend.domains.Operacao;
import com.apiexternabackend.domains.enums.Mercado;
import com.apiexternabackend.domains.enums.TipoOperacao;
import com.apiexternabackend.repositories.CarteiraAcaoRepository;
import com.apiexternabackend.repositories.OperacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PosicaoServiceTest {

    @Mock private CarteiraAcaoRepository carteiraAcaoRepository;
    @Mock private OperacaoRepository operacaoRepository;

    @InjectMocks
    private PosicaoService service;

    private Carteira carteira;
    private Acao acao;

    @BeforeEach
    void setUp() {
        carteira = new Carteira();
        carteira.setId(1L);
        acao = new Acao(1L, "PETR4", "Petrobras", Mercado.BR, "BRL", null, null, true);
    }

    private Operacao compra(int qty, String preco) {
        Operacao op = new Operacao();
        op.setCarteira(carteira);
        op.setAcao(acao);
        op.setTipo(TipoOperacao.COMPRA);
        op.setQuantidade(qty);
        op.setPrecoUnitario(new BigDecimal(preco));
        op.setDataHora(LocalDateTime.now());
        return op;
    }

    private Operacao venda(int qty, String preco) {
        Operacao op = new Operacao();
        op.setCarteira(carteira);
        op.setAcao(acao);
        op.setTipo(TipoOperacao.VENDA);
        op.setQuantidade(qty);
        op.setPrecoUnitario(new BigDecimal(preco));
        op.setDataHora(LocalDateTime.now());
        return op;
    }

    @Test
    @DisplayName("@spec:AC-402 Compras sucessivas geram preço médio ponderado (100@38 + 100@42 = 200@40)")
    void devecalcularPrecoMedioPonderado() {
        when(operacaoRepository.findByCarteiraIdAndAcaoIdAndAtivoTrueOrderByDataHoraAsc(1L, 1L))
                .thenReturn(List.of(compra(100, "38"), compra(100, "42")));
        when(carteiraAcaoRepository.findByCarteiraIdAndAcaoId(1L, 1L)).thenReturn(Optional.empty());

        service.recalcular(carteira, acao);

        ArgumentCaptor<CarteiraAcao> captor = ArgumentCaptor.forClass(CarteiraAcao.class);
        verify(carteiraAcaoRepository).save(captor.capture());
        assertThat(captor.getValue().getQuantidade()).isEqualTo(200);
        assertThat(captor.getValue().getPrecoMedio()).isEqualByComparingTo("40.0000");
    }

    @Test
    @DisplayName("@spec:AC-405 Venda que zera a posição remove a posição")
    void deveRemoverPosicaoQuandoZerada() {
        CarteiraAcao posicao = new CarteiraAcao(1L, carteira, acao, 100, new BigDecimal("38"));
        when(operacaoRepository.findByCarteiraIdAndAcaoIdAndAtivoTrueOrderByDataHoraAsc(1L, 1L))
                .thenReturn(List.of(compra(100, "38"), venda(100, "42")));
        when(carteiraAcaoRepository.findByCarteiraIdAndAcaoId(1L, 1L))
                .thenReturn(Optional.of(posicao));

        service.recalcular(carteira, acao);

        verify(carteiraAcaoRepository).delete(posicao);
    }

    @Test
    @DisplayName("@spec:AC-412 Editar lançamento recalcula posição a partir do histórico atualizado")
    void deveRecalcularAoEditarLancamento() {
        when(operacaoRepository.findByCarteiraIdAndAcaoIdAndAtivoTrueOrderByDataHoraAsc(1L, 1L))
                .thenReturn(List.of(compra(50, "40")));
        when(carteiraAcaoRepository.findByCarteiraIdAndAcaoId(1L, 1L)).thenReturn(Optional.empty());

        service.recalcular(carteira, acao);

        ArgumentCaptor<CarteiraAcao> captor = ArgumentCaptor.forClass(CarteiraAcao.class);
        verify(carteiraAcaoRepository).save(captor.capture());
        assertThat(captor.getValue().getQuantidade()).isEqualTo(50);
        assertThat(captor.getValue().getPrecoMedio()).isEqualByComparingTo("40");
    }

    @Test
    @DisplayName("@spec:AC-463 Preço médio correto misturando compra automática e compra manual (100@38 auto + 100@42 manual = 200@40)")
    void devecalcularPrecoMedioMisturandoAutomaticaEManual() {
        Operacao automatica = compra(100, "38");
        automatica.setPrecoManual(false);
        automatica.setCotacaoNoMomento(new BigDecimal("38"));

        Operacao manual = compra(100, "42");
        manual.setPrecoManual(true);
        manual.setCotacaoNoMomento(new BigDecimal("38.50")); // cotação real no momento, diferente do preço manual informado

        when(operacaoRepository.findByCarteiraIdAndAcaoIdAndAtivoTrueOrderByDataHoraAsc(1L, 1L))
                .thenReturn(List.of(automatica, manual));
        when(carteiraAcaoRepository.findByCarteiraIdAndAcaoId(1L, 1L)).thenReturn(Optional.empty());

        service.recalcular(carteira, acao);

        ArgumentCaptor<CarteiraAcao> captor = ArgumentCaptor.forClass(CarteiraAcao.class);
        verify(carteiraAcaoRepository).save(captor.capture());
        assertThat(captor.getValue().getQuantidade()).isEqualTo(200);
        assertThat(captor.getValue().getPrecoMedio()).isEqualByComparingTo("40.0000");
    }

    @Test
    @DisplayName("@spec:AC-413 Excluir lançamento recalcula posição; sem quantidade = posição removida")
    void deveRemoverPosicaoAoExcluirUnicoLancamento() {
        CarteiraAcao posicao = new CarteiraAcao(1L, carteira, acao, 100, new BigDecimal("38"));
        when(operacaoRepository.findByCarteiraIdAndAcaoIdAndAtivoTrueOrderByDataHoraAsc(1L, 1L))
                .thenReturn(List.of());  // histórico vazio após exclusão
        when(carteiraAcaoRepository.findByCarteiraIdAndAcaoId(1L, 1L))
                .thenReturn(Optional.of(posicao));

        service.recalcular(carteira, acao);

        verify(carteiraAcaoRepository).delete(posicao);
    }

    @Test
    @DisplayName("@spec:AC-469 Venda grava preço médio de compra e lucro realizado do momento")
    void deveGravarPrecoMedioELucroRealizadoNaVenda() {
        Operacao compra = compra(100, "38");
        Operacao venda = venda(40, "45");
        when(operacaoRepository.findByCarteiraIdAndAcaoIdAndAtivoTrueOrderByDataHoraAsc(1L, 1L))
                .thenReturn(List.of(compra, venda));
        when(carteiraAcaoRepository.findByCarteiraIdAndAcaoId(1L, 1L)).thenReturn(Optional.empty());

        service.recalcular(carteira, acao);

        assertThat(venda.getPrecoMedioCompraNoMomento()).isEqualByComparingTo("38.0000");
        assertThat(venda.getLucroRealizado()).isEqualByComparingTo("280.0000"); // (45-38)*40
        verify(operacaoRepository).save(venda);
    }

    @Test
    @DisplayName("@spec:AC-474 Excluir uma venda recalcula o lucro realizado da venda restante")
    void deveRecalcularLucroRestanteAoExcluirVendaAnterior() {
        // compra 100@30 -> venda 100@40 (zera) -> compra 100@50 -> venda 50@60
        // a venda intermediária já foi excluída (soft delete) — não aparece no histórico ativo
        Operacao compra1 = compra(100, "30");
        Operacao compra2 = compra(100, "50");
        Operacao vendaRestante = venda(50, "60");
        when(operacaoRepository.findByCarteiraIdAndAcaoIdAndAtivoTrueOrderByDataHoraAsc(1L, 1L))
                .thenReturn(List.of(compra1, compra2, vendaRestante));
        when(carteiraAcaoRepository.findByCarteiraIdAndAcaoId(1L, 1L)).thenReturn(Optional.empty());

        service.recalcular(carteira, acao);

        // preço médio misto (100@30 + 100@50)/200 = 40 — diferente do que seria se a venda excluída ainda contasse
        assertThat(vendaRestante.getPrecoMedioCompraNoMomento()).isEqualByComparingTo("40.0000");
        assertThat(vendaRestante.getLucroRealizado()).isEqualByComparingTo("1000.0000"); // (60-40)*50
    }

    @Test
    @DisplayName("@spec:AC-475 Editar preço/quantidade de uma venda recalcula o próprio lucro realizado")
    void deveRecalcularLucroDaPropriaVendaAoEditar() {
        Operacao compraBase = compra(100, "30");
        Operacao vendaEditavel = venda(50, "50");
        when(operacaoRepository.findByCarteiraIdAndAcaoIdAndAtivoTrueOrderByDataHoraAsc(1L, 1L))
                .thenReturn(List.of(compraBase, vendaEditavel));
        when(carteiraAcaoRepository.findByCarteiraIdAndAcaoId(1L, 1L)).thenReturn(Optional.empty());

        service.recalcular(carteira, acao);
        assertThat(vendaEditavel.getLucroRealizado()).isEqualByComparingTo("1000.0000"); // (50-30)*50

        vendaEditavel.setPrecoUnitario(new BigDecimal("70")); // simula edição do preço da própria venda
        service.recalcular(carteira, acao);
        assertThat(vendaEditavel.getPrecoMedioCompraNoMomento()).isEqualByComparingTo("30.0000");
        assertThat(vendaEditavel.getLucroRealizado()).isEqualByComparingTo("2000.0000"); // (70-30)*50
    }

    @Test
    @DisplayName("@spec:AC-476 Editar uma compra recalcula em cascata o lucro de vendas posteriores")
    void deveRecalcularLucroDeVendaPosteriorAoEditarCompraAnterior() {
        Operacao compraEditavel = compra(100, "30");
        Operacao venda = venda(50, "50");
        when(operacaoRepository.findByCarteiraIdAndAcaoIdAndAtivoTrueOrderByDataHoraAsc(1L, 1L))
                .thenReturn(List.of(compraEditavel, venda));
        when(carteiraAcaoRepository.findByCarteiraIdAndAcaoId(1L, 1L)).thenReturn(Optional.empty());

        service.recalcular(carteira, acao);
        assertThat(venda.getLucroRealizado()).isEqualByComparingTo("1000.0000"); // (50-30)*50

        compraEditavel.setPrecoUnitario(new BigDecimal("20")); // simula edição do preço da compra
        service.recalcular(carteira, acao);
        assertThat(venda.getLucroRealizado()).isEqualByComparingTo("1500.0000"); // (50-20)*50
    }
}
