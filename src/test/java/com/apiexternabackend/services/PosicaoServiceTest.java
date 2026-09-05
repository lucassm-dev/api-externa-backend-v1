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
        when(operacaoRepository.findByCarteiraIdAndAcaoIdOrderByDataHoraAsc(1L, 1L))
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
        when(operacaoRepository.findByCarteiraIdAndAcaoIdOrderByDataHoraAsc(1L, 1L))
                .thenReturn(List.of(compra(100, "38"), venda(100, "42")));
        when(carteiraAcaoRepository.findByCarteiraIdAndAcaoId(1L, 1L))
                .thenReturn(Optional.of(posicao));

        service.recalcular(carteira, acao);

        verify(carteiraAcaoRepository).delete(posicao);
    }

    @Test
    @DisplayName("@spec:AC-412 Editar lançamento recalcula posição a partir do histórico atualizado")
    void deveRecalcularAoEditarLancamento() {
        when(operacaoRepository.findByCarteiraIdAndAcaoIdOrderByDataHoraAsc(1L, 1L))
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

        when(operacaoRepository.findByCarteiraIdAndAcaoIdOrderByDataHoraAsc(1L, 1L))
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
        when(operacaoRepository.findByCarteiraIdAndAcaoIdOrderByDataHoraAsc(1L, 1L))
                .thenReturn(List.of());  // histórico vazio após exclusão
        when(carteiraAcaoRepository.findByCarteiraIdAndAcaoId(1L, 1L))
                .thenReturn(Optional.of(posicao));

        service.recalcular(carteira, acao);

        verify(carteiraAcaoRepository).delete(posicao);
    }
}
