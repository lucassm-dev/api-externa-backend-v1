package com.apiexternabackend.services;

import com.apiexternabackend.domains.Acao;
import com.apiexternabackend.domains.dtos.AcaoRequestDTO;
import com.apiexternabackend.domains.dtos.AcaoResponseDTO;
import com.apiexternabackend.domains.enums.Mercado;
import com.apiexternabackend.infra.adapter.BrapiAdapter;
import com.apiexternabackend.infra.adapter.CotacaoResultado;
import com.apiexternabackend.infra.adapter.TwelveDataAdapter;
import com.apiexternabackend.mappers.AcaoMapper;
import com.apiexternabackend.repositories.AcaoRepository;
import com.apiexternabackend.resources.exceptions.IntegracaoExternaException;
import com.apiexternabackend.resources.exceptions.RecursoDuplicadoException;
import com.apiexternabackend.resources.exceptions.RecursoNaoEncontradoException;
import com.apiexternabackend.resources.exceptions.RegraVioladaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcaoServiceTest {

    private static final Long INVESTIDOR_ID = 1L;

    @Mock private AcaoRepository repository;
    @Mock private com.apiexternabackend.repositories.CarteiraAcaoRepository carteiraAcaoRepository;
    @Mock private com.apiexternabackend.repositories.CarteiraRepository carteiraRepository;
    @Mock private AcaoMapper mapper;
    @Mock private BrapiAdapter brapiAdapter;
    @Mock private TwelveDataAdapter twelveDataAdapter;
    @Mock private CotacaoCacheService cotacaoCacheService;

    @InjectMocks
    private AcaoService service;

    private Acao acaoBR;
    private Acao acaoUS;
    private AcaoResponseDTO responseBR;
    private AcaoResponseDTO responseUS;
    private CotacaoResultado cotacaoBR;
    private CotacaoResultado cotacaoUS;

    @BeforeEach
    void setUp() {
        lenient().when(carteiraRepository.existsByInvestidorIdAndAtivaTrue(INVESTIDOR_ID)).thenReturn(true);
        LocalDateTime agora = LocalDateTime.now();
        cotacaoBR = new CotacaoResultado(new BigDecimal("38.00"), agora);
        cotacaoUS = new CotacaoResultado(new BigDecimal("150.00"), agora);

        acaoBR = new Acao(1L, "PETR4", "Petrobras", Mercado.BR, "BRL", new BigDecimal("38.00"), agora, true);
        acaoUS = new Acao(2L, "AAPL", "Apple Inc.", Mercado.US, "USD", new BigDecimal("150.00"), agora, true);

        responseBR = new AcaoResponseDTO(1L, "PETR4", "Petrobras", Mercado.BR, "BRL", new BigDecimal("38.00"), agora);
        responseUS = new AcaoResponseDTO(2L, "AAPL", "Apple Inc.", Mercado.US, "USD", new BigDecimal("150.00"), agora);
    }

    @Test
    @DisplayName("@spec:AC-201 Ticker brasileiro válido é cadastrado com cotação BR e moeda BRL")
    void deveCadastrarAcaoBrasileira() {
        when(repository.existsByTickerAndAtivoTrue("PETR4")).thenReturn(false);
        when(brapiAdapter.buscarCotacao("PETR4")).thenReturn(cotacaoBR);
        when(repository.save(any())).thenReturn(acaoBR);
        when(mapper.toResponse(acaoBR)).thenReturn(responseBR);

        AcaoResponseDTO result = service.cadastrar(new AcaoRequestDTO("PETR4", Mercado.BR), INVESTIDOR_ID);

        assertThat(result.getMercado()).isEqualTo(Mercado.BR);
        assertThat(result.getMoeda()).isEqualTo("BRL");
        assertThat(result.getCotacaoAtual()).isEqualByComparingTo("38.00");
        assertThat(result.getDataHoraCotacao()).isNotNull();
    }

    @Test
    @DisplayName("@spec:AC-202 Ticker americano usa a fonte US (Twelve Data) e moeda USD")
    void deveCadastrarAcaoAmericana() {
        when(repository.existsByTickerAndAtivoTrue("AAPL")).thenReturn(false);
        when(twelveDataAdapter.buscarCotacao("AAPL")).thenReturn(cotacaoUS);
        when(repository.save(any())).thenReturn(acaoUS);
        when(mapper.toResponse(acaoUS)).thenReturn(responseUS);

        AcaoResponseDTO result = service.cadastrar(new AcaoRequestDTO("AAPL", Mercado.US), INVESTIDOR_ID);

        assertThat(result.getMercado()).isEqualTo(Mercado.US);
        assertThat(result.getMoeda()).isEqualTo("USD");
        verify(twelveDataAdapter).buscarCotacao("AAPL");
        verify(brapiAdapter, never()).buscarCotacao(any());
    }

    @Test
    @DisplayName("@spec:AC-203 Ticker inexistente na fonte impede o cadastro")
    void deveRejeitarTickerInexistente() {
        when(repository.existsByTickerAndAtivoTrue("XXXX3")).thenReturn(false);
        when(brapiAdapter.buscarCotacao("XXXX3"))
                .thenThrow(new RegraVioladaException("EXT-008", "Ticker não encontrado na fonte BR: XXXX3"));

        assertThatThrownBy(() -> service.cadastrar(new AcaoRequestDTO("XXXX3", Mercado.BR), INVESTIDOR_ID))
                .isInstanceOf(RegraVioladaException.class)
                .hasMessageContaining("não encontrado");
    }

    @Test
    @DisplayName("@spec:AC-204 @spec:AC-446 Ticker duplicado entre ativos é impedido")
    void deveRejeitarTickerDuplicado() {
        when(repository.existsByTickerAndAtivoTrue("PETR4")).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrar(new AcaoRequestDTO("PETR4", Mercado.BR), INVESTIDOR_ID))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    @DisplayName("@spec:AC-205 Mercado BR usa fonte BR, mercado US usa fonte US — nunca a fonte errada")
    void deveRotearParaFonteCorretaConforme() {
        when(repository.existsByTickerAndAtivoTrue("PETR4")).thenReturn(false);
        when(brapiAdapter.buscarCotacao("PETR4")).thenReturn(cotacaoBR);
        when(repository.save(any())).thenReturn(acaoBR);
        when(mapper.toResponse(acaoBR)).thenReturn(responseBR);
        service.cadastrar(new AcaoRequestDTO("PETR4", Mercado.BR), INVESTIDOR_ID);
        verify(brapiAdapter).buscarCotacao("PETR4");
        verify(twelveDataAdapter, never()).buscarCotacao(any());
    }

    @Test
    @DisplayName("@spec:AC-206 Listar ações retorna página")
    void deveListarAcoesPaginadas() {
        when(repository.findAllByAtivoTrue(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(acaoBR)));
        when(mapper.toResponse(acaoBR)).thenReturn(responseBR);

        Page<AcaoResponseDTO> result = service.listar(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("@spec:AC-207 Buscar por ticker existente retorna dados")
    void deveBuscarPorTickerExistente() {
        when(repository.findByTickerAndAtivoTrue("PETR4")).thenReturn(Optional.of(acaoBR));
        when(mapper.toResponse(acaoBR)).thenReturn(responseBR);

        AcaoResponseDTO result = service.buscarPorTicker("PETR4");

        assertThat(result.getTicker()).isEqualTo("PETR4");
    }

    @Test
    @DisplayName("@spec:AC-207 @spec:AC-448 Buscar por ticker inexistente ou inativo lança não encontrado")
    void deveLancarNotFoundParaTickerInexistente() {
        when(repository.findByTickerAndAtivoTrue("XXXX3")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorTicker("XXXX3"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("@spec:AC-208 Atualizar cotação busca novo preço e atualiza data/hora")
    void deveAtualizarCotacao() {
        CotacaoResultado novaCotacao = new CotacaoResultado(new BigDecimal("42.00"), LocalDateTime.now());
        when(repository.findById(1L)).thenReturn(Optional.of(acaoBR));
        when(cotacaoCacheService.obter(acaoBR, false)).thenReturn(novaCotacao);
        when(mapper.toResponse(acaoBR)).thenReturn(responseBR);

        AcaoResponseDTO result = service.atualizarCotacao(1L, false);

        assertThat(result).isNotNull();
        verify(cotacaoCacheService).obter(acaoBR, false);
    }

    @Test
    @DisplayName("@spec:AC-481 Forçar atualização ignora o cache mesmo com TTL válido")
    void deveForcarAtualizacaoIgnorandoCache() {
        when(repository.findById(1L)).thenReturn(Optional.of(acaoBR));
        when(cotacaoCacheService.obter(acaoBR, true)).thenReturn(cotacaoBR);
        when(mapper.toResponse(acaoBR)).thenReturn(responseBR);

        service.atualizarCotacao(1L, true);

        verify(cotacaoCacheService).obter(acaoBR, true);
    }

    @Test
    @DisplayName("@spec:AC-209 Cotação sempre acompanha data/hora de obtenção")
    void deveCotacaoAcompanharDataHora() {
        when(repository.existsByTickerAndAtivoTrue("PETR4")).thenReturn(false);
        when(brapiAdapter.buscarCotacao("PETR4")).thenReturn(cotacaoBR);
        when(repository.save(any())).thenReturn(acaoBR);
        when(mapper.toResponse(acaoBR)).thenReturn(responseBR);

        AcaoResponseDTO result = service.cadastrar(new AcaoRequestDTO("PETR4", Mercado.BR), INVESTIDOR_ID);

        assertThat(result.getDataHoraCotacao()).isNotNull();
    }

    @Test
    @DisplayName("@spec:AC-210 @spec:AC-432 Fonte indisponível (não é cota) retorna última cotação conhecida, preserva comportamento atual")
    void deveRetornarUltimaCotacaoQuandoFonteIndisponivel() {
        when(repository.findById(1L)).thenReturn(Optional.of(acaoBR));
        when(cotacaoCacheService.obter(acaoBR, false))
                .thenThrow(new IntegracaoExternaException("EXT-010", "Fonte BR indisponível", false));
        when(mapper.toResponse(acaoBR)).thenReturn(responseBR);

        // Não deve lançar exceção — retorna a última cotação conhecida
        AcaoResponseDTO result = service.atualizarCotacao(1L, false);

        assertThat(result).isNotNull();
        assertThat(result.getCotacaoAtual()).isEqualByComparingTo("38.00");
    }

    @Test
    @DisplayName("@spec:AC-211 @spec:AC-429 Cota excedida resulta em exceção mapeada para 429 com mensagem de limite")
    void deveMensagemEspecificaParaLimiteExcedido() {
        when(repository.existsByTickerAndAtivoTrue("PETR4")).thenReturn(false);
        when(brapiAdapter.buscarCotacao("PETR4"))
                .thenThrow(new IntegracaoExternaException("EXT-009", "Limite de requisições da fonte BR excedido", true));

        assertThatThrownBy(() -> service.cadastrar(new AcaoRequestDTO("PETR4", Mercado.BR), INVESTIDOR_ID))
                .isInstanceOf(IntegracaoExternaException.class)
                .hasMessageContaining("Limite")
                .extracting(e -> ((IntegracaoExternaException) e).getHttpStatus())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("@spec:AC-431 @spec:AC-485 Atualizar cotação com cota estourada propaga 429, não silencia devolvendo a cotação antiga")
    void deveLancarExcecaoDe429AoAtualizarCotacaoComCotaEstourada() {
        when(repository.findById(1L)).thenReturn(Optional.of(acaoBR));
        when(cotacaoCacheService.obter(acaoBR, false))
                .thenThrow(new IntegracaoExternaException("EXT-009", "Limite de requisições da fonte BR excedido", true));

        assertThatThrownBy(() -> service.atualizarCotacao(1L, false))
                .isInstanceOf(IntegracaoExternaException.class)
                .extracting(e -> ((IntegracaoExternaException) e).getHttpStatus())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("@spec:AC-433 Exceções de Ação carregam código do catálogo (ACA-001/ACA-002)")
    void deveExcecoesDeAcaoCarregaremCodigoDoCatalogo() {
        when(repository.existsByTickerAndAtivoTrue("PETR4")).thenReturn(true);
        assertThatThrownBy(() -> service.cadastrar(new AcaoRequestDTO("PETR4", Mercado.BR), INVESTIDOR_ID))
                .isInstanceOf(RecursoDuplicadoException.class)
                .extracting(e -> ((RecursoDuplicadoException) e).getCodigo())
                .isEqualTo("ACA-002");

        when(repository.findByTickerAndAtivoTrue("XXXX3")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.buscarPorTicker("XXXX3"))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .extracting(e -> ((RecursoNaoEncontradoException) e).getCodigo())
                .isEqualTo("ACA-001");
    }

    @Test
    @DisplayName("@spec:AC-424 Ação ativa é desativada ao excluir pelo ticker")
    void deveDesativarAcaoAoExcluir() {
        when(repository.findByTickerAndAtivoTrue("PETR4")).thenReturn(Optional.of(acaoBR));
        when(carteiraAcaoRepository.countByAcaoIdAndQuantidadeGreaterThan(acaoBR.getId(), 0)).thenReturn(0L);

        service.excluir("PETR4");

        verify(repository).save(acaoBR);
        assertThat(acaoBR.getAtivo()).isFalse();
    }

    @Test
    @DisplayName("@spec:AC-425 Excluir ação inexistente ou inativa lança RecursoNaoEncontradoException")
    void deveLancarNotFoundAoExcluirAcaoInexistente() {
        when(repository.findByTickerAndAtivoTrue("XXXX3")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.excluir("XXXX3"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("@spec:AC-444 Recadastrar ticker de ação excluída funciona (verificação de duplicidade só considera ativas)")
    void deveRecadastrarTickerDeAcaoExcluida() {
        when(repository.existsByTickerAndAtivoTrue("PETR4")).thenReturn(false);
        when(brapiAdapter.buscarCotacao("PETR4")).thenReturn(cotacaoBR);
        when(repository.save(any())).thenReturn(acaoBR);
        when(mapper.toResponse(acaoBR)).thenReturn(responseBR);

        AcaoResponseDTO result = service.cadastrar(new AcaoRequestDTO("PETR4", Mercado.BR), INVESTIDOR_ID);

        assertThat(result).isNotNull();
        verify(repository).existsByTickerAndAtivoTrue("PETR4");
    }

    @Test
    @DisplayName("@spec:AC-447 Recadastro cria linha nova — nunca reativa/reaproveita a linha antiga (histórico não é ressuscitado)")
    void deveRecadastroSempreCriarLinhaNovaNuncaReativarAntiga() {
        when(repository.existsByTickerAndAtivoTrue("PETR4")).thenReturn(false);
        when(brapiAdapter.buscarCotacao("PETR4")).thenReturn(cotacaoBR);
        when(repository.save(any())).thenReturn(acaoBR);
        when(mapper.toResponse(acaoBR)).thenReturn(responseBR);

        service.cadastrar(new AcaoRequestDTO("PETR4", Mercado.BR), INVESTIDOR_ID);

        org.mockito.ArgumentCaptor<Acao> captor = org.mockito.ArgumentCaptor.forClass(Acao.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
    }

    @Test
    @DisplayName("@spec:AC-450 Excluir ação com posição ativa (quantidade > 0) é bloqueado")
    void deveBloquearExclusaoComPosicaoAtiva() {
        when(repository.findByTickerAndAtivoTrue("PETR4")).thenReturn(Optional.of(acaoBR));
        when(carteiraAcaoRepository.countByAcaoIdAndQuantidadeGreaterThan(acaoBR.getId(), 0)).thenReturn(2L);

        assertThatThrownBy(() -> service.excluir("PETR4"))
                .isInstanceOf(com.apiexternabackend.resources.exceptions.RegraVioladaException.class)
                .hasMessageContaining("2");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("@spec:AC-451 Excluir ação com posições zeradas é permitido")
    void devePermitirExclusaoComPosicoesZeradas() {
        when(repository.findByTickerAndAtivoTrue("PETR4")).thenReturn(Optional.of(acaoBR));
        when(carteiraAcaoRepository.countByAcaoIdAndQuantidadeGreaterThan(acaoBR.getId(), 0)).thenReturn(0L);

        service.excluir("PETR4");

        verify(repository).save(acaoBR);
        assertThat(acaoBR.getAtivo()).isFalse();
    }

    @Test
    @DisplayName("@spec:AC-466 Investidor sem carteira ativa não cadastra ação")
    void deveRejeitarCadastroSemCarteiraAtiva() {
        when(carteiraRepository.existsByInvestidorIdAndAtivaTrue(INVESTIDOR_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.cadastrar(new AcaoRequestDTO("PETR4", Mercado.BR), INVESTIDOR_ID))
                .isInstanceOf(com.apiexternabackend.resources.exceptions.PreRequisitoNaoAtendidoException.class)
                .extracting(e -> ((com.apiexternabackend.resources.exceptions.PreRequisitoNaoAtendidoException) e).getCodigo())
                .isEqualTo("ACA-004");

        verify(repository, never()).save(any());
        verify(brapiAdapter, never()).buscarCotacao(any());
    }

    @Test
    @DisplayName("@spec:AC-467 Investidor com carteira ativa cadastra normalmente")
    void devePermitirCadastroComCarteiraAtiva() {
        when(carteiraRepository.existsByInvestidorIdAndAtivaTrue(INVESTIDOR_ID)).thenReturn(true);
        when(repository.existsByTickerAndAtivoTrue("PETR4")).thenReturn(false);
        when(brapiAdapter.buscarCotacao("PETR4")).thenReturn(cotacaoBR);
        when(repository.save(any())).thenReturn(acaoBR);
        when(mapper.toResponse(acaoBR)).thenReturn(responseBR);

        AcaoResponseDTO result = service.cadastrar(new AcaoRequestDTO("PETR4", Mercado.BR), INVESTIDOR_ID);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("@spec:AC-468 Mensagem de erro orienta cadastrar uma carteira antes")
    void deveOrientarProximoPassoNaMensagemDeErro() {
        when(carteiraRepository.existsByInvestidorIdAndAtivaTrue(INVESTIDOR_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.cadastrar(new AcaoRequestDTO("PETR4", Mercado.BR), INVESTIDOR_ID))
                .hasMessageContaining("Cadastre uma carteira antes de cadastrar ações");
    }
}
