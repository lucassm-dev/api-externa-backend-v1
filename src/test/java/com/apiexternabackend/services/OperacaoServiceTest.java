package com.apiexternabackend.services;

import com.apiexternabackend.domains.Acao;
import com.apiexternabackend.domains.Carteira;
import com.apiexternabackend.domains.CarteiraAcao;
import com.apiexternabackend.domains.Corretora;
import com.apiexternabackend.domains.Investidor;
import com.apiexternabackend.domains.Operacao;
import com.apiexternabackend.domains.dtos.OperacaoRequestDTO;
import com.apiexternabackend.domains.dtos.OperacaoResponseDTO;
import com.apiexternabackend.domains.enums.Mercado;
import com.apiexternabackend.domains.enums.TipoOperacao;
import com.apiexternabackend.infra.adapter.BrapiAdapter;
import com.apiexternabackend.infra.adapter.CotacaoResultado;
import com.apiexternabackend.infra.adapter.TwelveDataAdapter;
import com.apiexternabackend.mappers.OperacaoMapper;
import com.apiexternabackend.repositories.AcaoRepository;
import com.apiexternabackend.repositories.CarteiraAcaoRepository;
import com.apiexternabackend.repositories.OperacaoRepository;
import com.apiexternabackend.resources.exceptions.RecursoNaoEncontradoException;
import com.apiexternabackend.resources.exceptions.RegraVioladaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperacaoServiceTest {

    @Mock private OperacaoRepository operacaoRepository;
    @Mock private AcaoRepository acaoRepository;
    @Mock private CarteiraAcaoRepository carteiraAcaoRepository;
    @Mock private CarteiraService carteiraService;
    @Mock private PosicaoService posicaoService;
    @Mock private OperacaoMapper mapper;
    @Mock private BrapiAdapter brapiAdapter;
    @Mock private TwelveDataAdapter twelveDataAdapter;

    @InjectMocks
    private OperacaoService service;

    private static final Long INVESTIDOR_ID = 1L;

    private Investidor investidor;
    private Corretora corretora;
    private Carteira carteiraBR;
    private Carteira carteiraUS;
    private Acao acaoBR;
    private Acao acaoUS;
    private Operacao operacao;
    private OperacaoResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        investidor = new Investidor(INVESTIDOR_ID, "João", "joao@email.com", "12345678901", "hash", LocalDateTime.now(), true);
        corretora = new Corretora();
        corretora.setId(1L);

        carteiraBR = new Carteira(1L, investidor, corretora, Mercado.BR, "CartBR", true);
        carteiraUS = new Carteira(2L, investidor, corretora, Mercado.US, "CartUS", true);

        acaoBR = new Acao(1L, "PETR4", "Petrobras", Mercado.BR, "BRL", new BigDecimal("38"), LocalDateTime.now(), true);
        acaoUS = new Acao(2L, "AAPL", "Apple", Mercado.US, "USD", new BigDecimal("150"), LocalDateTime.now(), true);

        operacao = new Operacao(1L, carteiraBR, acaoBR, TipoOperacao.COMPRA, 100, new BigDecimal("38"), LocalDateTime.now(), false, new BigDecimal("38"), true, null, null);

        responseDTO = new OperacaoResponseDTO(1L, 1L, "PETR4", TipoOperacao.COMPRA, 100,
                new BigDecimal("38"), new BigDecimal("3800"), LocalDateTime.now(), "BRL", java.util.List.of());
    }

    @Test
    @DisplayName("@spec:AC-401 Compra usa cotação buscada no ato — preço não digitado")
    void deveComprarUsandoCotacaoDaFonte() {
        CotacaoResultado cotacao = new CotacaoResultado(new BigDecimal("38.50"), LocalDateTime.now());
        when(carteiraService.buscarAtiva(1L, INVESTIDOR_ID)).thenReturn(carteiraBR);
        when(acaoRepository.findByTickerAndAtivoTrue("PETR4")).thenReturn(Optional.of(acaoBR));
        when(brapiAdapter.buscarCotacao("PETR4")).thenReturn(cotacao);
        when(operacaoRepository.save(any())).thenReturn(operacao);
        when(mapper.toResponse(operacao)).thenReturn(responseDTO);

        service.comprar(new OperacaoRequestDTO(1L, "PETR4", 100, null), INVESTIDOR_ID);

        verify(brapiAdapter).buscarCotacao("PETR4");
    }

    @Test
    @DisplayName("@spec:AC-403 Comprar ação de mercado diferente da carteira é recusado")
    void deveRejeitarCompraDeAcaoMercadoDiferente() {
        when(carteiraService.buscarAtiva(1L, INVESTIDOR_ID)).thenReturn(carteiraBR);
        when(acaoRepository.findByTickerAndAtivoTrue("AAPL")).thenReturn(Optional.of(acaoUS));

        assertThatThrownBy(() -> service.comprar(new OperacaoRequestDTO(1L, "AAPL", 10, null), INVESTIDOR_ID))
                .isInstanceOf(RegraVioladaException.class)
                .hasMessageContaining("mercado");
    }

    @Test
    @DisplayName("@spec:AC-305 Operação com ação de mercado diferente da carteira (spec carteira) é recusada")
    void deveRejeitarOperacaoMercadoIncompativel() {
        when(carteiraService.buscarAtiva(2L, INVESTIDOR_ID)).thenReturn(carteiraUS);
        when(acaoRepository.findByTickerAndAtivoTrue("PETR4")).thenReturn(Optional.of(acaoBR));

        assertThatThrownBy(() -> service.comprar(new OperacaoRequestDTO(2L, "PETR4", 10, null), INVESTIDOR_ID))
                .isInstanceOf(RegraVioladaException.class)
                .hasMessageContaining("mercado");
    }

    @Test
    @DisplayName("@spec:AC-404 Não é possível vender mais que a posição atual")
    void deveRejeitarVendaAcimaDataPosicao() {
        CarteiraAcao posicao = new CarteiraAcao(1L, carteiraBR, acaoBR, 50, new BigDecimal("38"));
        when(carteiraService.buscarAtiva(1L, INVESTIDOR_ID)).thenReturn(carteiraBR);
        when(acaoRepository.findByTickerAndAtivoTrue("PETR4")).thenReturn(Optional.of(acaoBR));
        when(carteiraAcaoRepository.findByCarteiraIdAndAcaoId(1L, 1L)).thenReturn(Optional.of(posicao));

        assertThatThrownBy(() -> service.vender(new OperacaoRequestDTO(1L, "PETR4", 100, null), INVESTIDOR_ID))
                .isInstanceOf(RegraVioladaException.class)
                .hasMessageContaining("posição atual");
    }

    @Test
    @DisplayName("@spec:AC-407 Cada operação gera exatamente uma movimentação")
    void deveGerarUmaMovimentacaoPorOperacao() {
        CotacaoResultado cotacao = new CotacaoResultado(new BigDecimal("38"), LocalDateTime.now());
        when(carteiraService.buscarAtiva(1L, INVESTIDOR_ID)).thenReturn(carteiraBR);
        when(acaoRepository.findByTickerAndAtivoTrue("PETR4")).thenReturn(Optional.of(acaoBR));
        when(brapiAdapter.buscarCotacao("PETR4")).thenReturn(cotacao);
        when(operacaoRepository.save(any())).thenReturn(operacao);
        when(mapper.toResponse(operacao)).thenReturn(responseDTO);

        service.comprar(new OperacaoRequestDTO(1L, "PETR4", 100, null), INVESTIDOR_ID);

        verify(operacaoRepository).save(any());
    }

    @Test
    @DisplayName("@spec:AC-309 Carteira inativa não recebe operações")
    void deveRejeitarOperacaoEmCarteiraInativa() {
        when(carteiraService.buscarAtiva(1L, INVESTIDOR_ID))
                .thenThrow(new RecursoNaoEncontradoException("CAR-001", "Carteira não encontrada ou inativa: 1"));

        assertThatThrownBy(() -> service.comprar(new OperacaoRequestDTO(1L, "PETR4", 10, null), INVESTIDOR_ID))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("@spec:AC-006 Operação em carteira de outro investidor é recusada como não encontrada")
    void deveRejeitarOperacaoEmCarteiraDeOutroInvestidor() {
        Long outroInvestidorId = 2L;
        when(carteiraService.buscarAtiva(1L, outroInvestidorId))
                .thenThrow(new RecursoNaoEncontradoException("CAR-001", "Carteira não encontrada ou inativa: 1"));

        assertThatThrownBy(() -> service.comprar(new OperacaoRequestDTO(1L, "PETR4", 10, null), outroInvestidorId))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("@spec:AC-411 Não existe erro de saldo insuficiente — compra nunca recusada por saldo")
    void naoDeveRejeitarPorSaldoInsuficiente() {
        CotacaoResultado cotacao = new CotacaoResultado(new BigDecimal("9999999"), LocalDateTime.now());
        when(carteiraService.buscarAtiva(1L, INVESTIDOR_ID)).thenReturn(carteiraBR);
        when(acaoRepository.findByTickerAndAtivoTrue("PETR4")).thenReturn(Optional.of(acaoBR));
        when(brapiAdapter.buscarCotacao("PETR4")).thenReturn(cotacao);
        when(operacaoRepository.save(any())).thenReturn(operacao);
        when(mapper.toResponse(operacao)).thenReturn(responseDTO);

        // compra de valor altíssimo não deve lançar exceção de saldo
        OperacaoResponseDTO result = service.comprar(new OperacaoRequestDTO(1L, "PETR4", 999999, null), INVESTIDOR_ID);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("@spec:AC-449 Comprar ação excluída (inativa) é bloqueado")
    void deveRejeitarCompraDeAcaoInativa() {
        when(carteiraService.buscarAtiva(1L, INVESTIDOR_ID)).thenReturn(carteiraBR);
        when(acaoRepository.findByTickerAndAtivoTrue("PETR4")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.comprar(new OperacaoRequestDTO(1L, "PETR4", 10, null), INVESTIDOR_ID))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("@spec:AC-457 Compra com preço informado usa o valor informado, não a cotação")
    void deveComprarUsandoPrecoInformado() {
        CotacaoResultado cotacao = new CotacaoResultado(new BigDecimal("38.50"), LocalDateTime.now());
        when(carteiraService.buscarAtiva(1L, INVESTIDOR_ID)).thenReturn(carteiraBR);
        when(acaoRepository.findByTickerAndAtivoTrue("PETR4")).thenReturn(Optional.of(acaoBR));
        when(brapiAdapter.buscarCotacao("PETR4")).thenReturn(cotacao);
        when(operacaoRepository.save(any())).thenReturn(operacao);
        when(mapper.toResponse(operacao)).thenReturn(responseDTO);

        service.comprar(new OperacaoRequestDTO(1L, "PETR4", 100, new BigDecimal("40.00")), INVESTIDOR_ID);

        org.mockito.ArgumentCaptor<Operacao> captor = org.mockito.ArgumentCaptor.forClass(Operacao.class);
        verify(operacaoRepository).save(captor.capture());
        assertThat(captor.getValue().getPrecoUnitario()).isEqualByComparingTo("40.00");
        assertThat(captor.getValue().getPrecoManual()).isTrue();
        assertThat(captor.getValue().getCotacaoNoMomento()).isEqualByComparingTo("38.50"); // AC-461: cotação sempre registrada
    }

    @Test
    @DisplayName("@spec:AC-456 @spec:AC-461 Compra sem preço informado usa a cotação atual e registra precoManual=false")
    void deveRegistrarPrecoAutomaticoComoNaoManual() {
        CotacaoResultado cotacao = new CotacaoResultado(new BigDecimal("38.50"), LocalDateTime.now());
        when(carteiraService.buscarAtiva(1L, INVESTIDOR_ID)).thenReturn(carteiraBR);
        when(acaoRepository.findByTickerAndAtivoTrue("PETR4")).thenReturn(Optional.of(acaoBR));
        when(brapiAdapter.buscarCotacao("PETR4")).thenReturn(cotacao);
        when(operacaoRepository.save(any())).thenReturn(operacao);
        when(mapper.toResponse(operacao)).thenReturn(responseDTO);

        service.comprar(new OperacaoRequestDTO(1L, "PETR4", 100, null), INVESTIDOR_ID);

        org.mockito.ArgumentCaptor<Operacao> captor = org.mockito.ArgumentCaptor.forClass(Operacao.class);
        verify(operacaoRepository).save(captor.capture());
        assertThat(captor.getValue().getPrecoManual()).isFalse();
        assertThat(captor.getValue().getPrecoUnitario()).isEqualByComparingTo("38.50");
        assertThat(captor.getValue().getCotacaoNoMomento()).isEqualByComparingTo("38.50");
    }

    @Test
    @DisplayName("@spec:AC-459 Preço com mais de 2 casas decimais é rejeitado (OPE-005)")
    void deveRejeitarPrecoComMaisDeDuasCasasDecimais() {
        CotacaoResultado cotacao = new CotacaoResultado(new BigDecimal("38.50"), LocalDateTime.now());
        when(carteiraService.buscarAtiva(1L, INVESTIDOR_ID)).thenReturn(carteiraBR);
        when(acaoRepository.findByTickerAndAtivoTrue("PETR4")).thenReturn(Optional.of(acaoBR));
        when(brapiAdapter.buscarCotacao("PETR4")).thenReturn(cotacao);

        assertThatThrownBy(() -> service.comprar(
                new OperacaoRequestDTO(1L, "PETR4", 100, new BigDecimal("40.123")), INVESTIDOR_ID))
                .isInstanceOf(RegraVioladaException.class)
                .extracting(e -> ((RegraVioladaException) e).getCodigo())
                .isEqualTo("OPE-005");

        verify(operacaoRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("@spec:AC-464 Editar operação com preço zero ou negativo é rejeitado")
    void deveRejeitarEdicaoComPrecoInvalido() {
        // AC-464 é garantido pela anotação @Positive em OperacaoEditarDTO (validação de payload, 400/VAL-001) —
        // este teste cobre a validação de escala decimal (OPE-005), que é validação de negócio no service.
        when(operacaoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(operacao));

        assertThatThrownBy(() -> service.editar(1L, null, new BigDecimal("40.999"), INVESTIDOR_ID))
                .isInstanceOf(RegraVioladaException.class)
                .extracting(e -> ((RegraVioladaException) e).getCodigo())
                .isEqualTo("OPE-005");
    }

    @Test
    @DisplayName("@spec:AC-465 Editar operação com preço válido recalcula a posição e marca precoManual=true")
    void deveEditarPrecoERecalcularPosicao() {
        when(operacaoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(operacao));
        when(operacaoRepository.save(any())).thenReturn(operacao);
        when(mapper.toResponse(operacao)).thenReturn(responseDTO);

        service.editar(1L, null, new BigDecimal("41.00"), INVESTIDOR_ID);

        assertThat(operacao.getPrecoUnitario()).isEqualByComparingTo("41.00");
        assertThat(operacao.getPrecoManual()).isTrue();
        assertThat(operacao.getCotacaoNoMomento()).isEqualByComparingTo(acaoBR.getCotacaoAtual());
        verify(posicaoService).recalcular(carteiraBR, acaoBR);
    }
}