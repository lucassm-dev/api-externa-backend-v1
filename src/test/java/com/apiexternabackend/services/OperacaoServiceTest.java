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

        operacao = new Operacao(1L, carteiraBR, acaoBR, TipoOperacao.COMPRA, 100, new BigDecimal("38"), LocalDateTime.now());

        responseDTO = new OperacaoResponseDTO(1L, 1L, "PETR4", TipoOperacao.COMPRA, 100,
                new BigDecimal("38"), new BigDecimal("3800"), LocalDateTime.now());
    }

    @Test
    @DisplayName("@spec:AC-401 Compra usa cotação buscada no ato — preço não digitado")
    void deveComprarUsandoCotacaoDaFonte() {
        CotacaoResultado cotacao = new CotacaoResultado(new BigDecimal("38.50"), LocalDateTime.now());
        when(carteiraService.buscarAtiva(1L, INVESTIDOR_ID)).thenReturn(carteiraBR);
        when(acaoRepository.findByTicker("PETR4")).thenReturn(Optional.of(acaoBR));
        when(brapiAdapter.buscarCotacao("PETR4")).thenReturn(cotacao);
        when(operacaoRepository.save(any())).thenReturn(operacao);
        when(mapper.toResponse(operacao)).thenReturn(responseDTO);

        service.comprar(new OperacaoRequestDTO(1L, "PETR4", 100), INVESTIDOR_ID);

        verify(brapiAdapter).buscarCotacao("PETR4");
    }

    @Test
    @DisplayName("@spec:AC-403 Comprar ação de mercado diferente da carteira é recusado")
    void deveRejeitarCompraDeAcaoMercadoDiferente() {
        when(carteiraService.buscarAtiva(1L, INVESTIDOR_ID)).thenReturn(carteiraBR);
        when(acaoRepository.findByTicker("AAPL")).thenReturn(Optional.of(acaoUS));

        assertThatThrownBy(() -> service.comprar(new OperacaoRequestDTO(1L, "AAPL", 10), INVESTIDOR_ID))
                .isInstanceOf(RegraVioladaException.class)
                .hasMessageContaining("mercado");
    }

    @Test
    @DisplayName("@spec:AC-305 Operação com ação de mercado diferente da carteira (spec carteira) é recusada")
    void deveRejeitarOperacaoMercadoIncompativel() {
        when(carteiraService.buscarAtiva(2L, INVESTIDOR_ID)).thenReturn(carteiraUS);
        when(acaoRepository.findByTicker("PETR4")).thenReturn(Optional.of(acaoBR));

        assertThatThrownBy(() -> service.comprar(new OperacaoRequestDTO(2L, "PETR4", 10), INVESTIDOR_ID))
                .isInstanceOf(RegraVioladaException.class)
                .hasMessageContaining("mercado");
    }

    @Test
    @DisplayName("@spec:AC-404 Não é possível vender mais que a posição atual")
    void deveRejeitarVendaAcimaDataPosicao() {
        CarteiraAcao posicao = new CarteiraAcao(1L, carteiraBR, acaoBR, 50, new BigDecimal("38"));
        when(carteiraService.buscarAtiva(1L, INVESTIDOR_ID)).thenReturn(carteiraBR);
        when(acaoRepository.findByTicker("PETR4")).thenReturn(Optional.of(acaoBR));
        when(carteiraAcaoRepository.findByCarteiraIdAndAcaoId(1L, 1L)).thenReturn(Optional.of(posicao));

        assertThatThrownBy(() -> service.vender(new OperacaoRequestDTO(1L, "PETR4", 100), INVESTIDOR_ID))
                .isInstanceOf(RegraVioladaException.class)
                .hasMessageContaining("posição atual");
    }

    @Test
    @DisplayName("@spec:AC-407 Cada operação gera exatamente uma movimentação")
    void deveGerarUmaMovimentacaoPorOperacao() {
        CotacaoResultado cotacao = new CotacaoResultado(new BigDecimal("38"), LocalDateTime.now());
        when(carteiraService.buscarAtiva(1L, INVESTIDOR_ID)).thenReturn(carteiraBR);
        when(acaoRepository.findByTicker("PETR4")).thenReturn(Optional.of(acaoBR));
        when(brapiAdapter.buscarCotacao("PETR4")).thenReturn(cotacao);
        when(operacaoRepository.save(any())).thenReturn(operacao);
        when(mapper.toResponse(operacao)).thenReturn(responseDTO);

        service.comprar(new OperacaoRequestDTO(1L, "PETR4", 100), INVESTIDOR_ID);

        verify(operacaoRepository).save(any());
    }

    @Test
    @DisplayName("@spec:AC-309 Carteira inativa não recebe operações")
    void deveRejeitarOperacaoEmCarteiraInativa() {
        when(carteiraService.buscarAtiva(1L, INVESTIDOR_ID))
                .thenThrow(new RecursoNaoEncontradoException("CAR-001", "Carteira não encontrada ou inativa: 1"));

        assertThatThrownBy(() -> service.comprar(new OperacaoRequestDTO(1L, "PETR4", 10), INVESTIDOR_ID))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("@spec:AC-006 Operação em carteira de outro investidor é recusada como não encontrada")
    void deveRejeitarOperacaoEmCarteiraDeOutroInvestidor() {
        Long outroInvestidorId = 2L;
        when(carteiraService.buscarAtiva(1L, outroInvestidorId))
                .thenThrow(new RecursoNaoEncontradoException("CAR-001", "Carteira não encontrada ou inativa: 1"));

        assertThatThrownBy(() -> service.comprar(new OperacaoRequestDTO(1L, "PETR4", 10), outroInvestidorId))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("@spec:AC-411 Não existe erro de saldo insuficiente — compra nunca recusada por saldo")
    void naoDeveRejeitarPorSaldoInsuficiente() {
        CotacaoResultado cotacao = new CotacaoResultado(new BigDecimal("9999999"), LocalDateTime.now());
        when(carteiraService.buscarAtiva(1L, INVESTIDOR_ID)).thenReturn(carteiraBR);
        when(acaoRepository.findByTicker("PETR4")).thenReturn(Optional.of(acaoBR));
        when(brapiAdapter.buscarCotacao("PETR4")).thenReturn(cotacao);
        when(operacaoRepository.save(any())).thenReturn(operacao);
        when(mapper.toResponse(operacao)).thenReturn(responseDTO);

        // compra de valor altíssimo não deve lançar exceção de saldo
        OperacaoResponseDTO result = service.comprar(new OperacaoRequestDTO(1L, "PETR4", 999999), INVESTIDOR_ID);
        assertThat(result).isNotNull();
    }
}