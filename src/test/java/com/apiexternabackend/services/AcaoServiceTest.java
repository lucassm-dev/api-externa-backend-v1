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
import com.apiexternabackend.resources.exceptions.BusinessException;
import com.apiexternabackend.resources.exceptions.DuplicateResourceException;
import com.apiexternabackend.resources.exceptions.ExternalServiceException;
import com.apiexternabackend.resources.exceptions.ResourceNotFoundException;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcaoServiceTest {

    @Mock private AcaoRepository repository;
    @Mock private AcaoMapper mapper;
    @Mock private BrapiAdapter brapiAdapter;
    @Mock private TwelveDataAdapter twelveDataAdapter;

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
        LocalDateTime agora = LocalDateTime.now();
        cotacaoBR = new CotacaoResultado(new BigDecimal("38.00"), agora);
        cotacaoUS = new CotacaoResultado(new BigDecimal("150.00"), agora);

        acaoBR = new Acao(1L, "PETR4", "Petrobras", Mercado.BR, "BRL", new BigDecimal("38.00"), agora);
        acaoUS = new Acao(2L, "AAPL", "Apple Inc.", Mercado.US, "USD", new BigDecimal("150.00"), agora);

        responseBR = new AcaoResponseDTO(1L, "PETR4", "Petrobras", Mercado.BR, "BRL", new BigDecimal("38.00"), agora);
        responseUS = new AcaoResponseDTO(2L, "AAPL", "Apple Inc.", Mercado.US, "USD", new BigDecimal("150.00"), agora);
    }

    @Test
    @DisplayName("@spec:AC-201 Ticker brasileiro válido é cadastrado com cotação BR e moeda BRL")
    void deveCadastrarAcaoBrasileira() {
        when(repository.existsByTicker("PETR4")).thenReturn(false);
        when(brapiAdapter.buscarCotacao("PETR4")).thenReturn(cotacaoBR);
        when(repository.save(any())).thenReturn(acaoBR);
        when(mapper.toResponse(acaoBR)).thenReturn(responseBR);

        AcaoResponseDTO result = service.cadastrar(new AcaoRequestDTO("PETR4", Mercado.BR));

        assertThat(result.getMercado()).isEqualTo(Mercado.BR);
        assertThat(result.getMoeda()).isEqualTo("BRL");
        assertThat(result.getCotacaoAtual()).isEqualByComparingTo("38.00");
        assertThat(result.getDataHoraCotacao()).isNotNull();
    }

    @Test
    @DisplayName("@spec:AC-202 Ticker americano usa a fonte US (Twelve Data) e moeda USD")
    void deveCadastrarAcaoAmericana() {
        when(repository.existsByTicker("AAPL")).thenReturn(false);
        when(twelveDataAdapter.buscarCotacao("AAPL")).thenReturn(cotacaoUS);
        when(repository.save(any())).thenReturn(acaoUS);
        when(mapper.toResponse(acaoUS)).thenReturn(responseUS);

        AcaoResponseDTO result = service.cadastrar(new AcaoRequestDTO("AAPL", Mercado.US));

        assertThat(result.getMercado()).isEqualTo(Mercado.US);
        assertThat(result.getMoeda()).isEqualTo("USD");
        verify(twelveDataAdapter).buscarCotacao("AAPL");
        verify(brapiAdapter, never()).buscarCotacao(any());
    }

    @Test
    @DisplayName("@spec:AC-203 Ticker inexistente na fonte impede o cadastro")
    void deveRejeitarTickerInexistente() {
        when(repository.existsByTicker("XXXX3")).thenReturn(false);
        when(brapiAdapter.buscarCotacao("XXXX3"))
                .thenThrow(new BusinessException("Ticker não encontrado na fonte BR: XXXX3"));

        assertThatThrownBy(() -> service.cadastrar(new AcaoRequestDTO("XXXX3", Mercado.BR)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não encontrado");
    }

    @Test
    @DisplayName("@spec:AC-204 Ticker duplicado é impedido")
    void deveRejeitarTickerDuplicado() {
        when(repository.existsByTicker("PETR4")).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrar(new AcaoRequestDTO("PETR4", Mercado.BR)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("@spec:AC-205 Mercado BR usa fonte BR, mercado US usa fonte US — nunca a fonte errada")
    void deveRotearParaFonteCorretaConforme() {
        when(repository.existsByTicker("PETR4")).thenReturn(false);
        when(brapiAdapter.buscarCotacao("PETR4")).thenReturn(cotacaoBR);
        when(repository.save(any())).thenReturn(acaoBR);
        when(mapper.toResponse(acaoBR)).thenReturn(responseBR);
        service.cadastrar(new AcaoRequestDTO("PETR4", Mercado.BR));
        verify(brapiAdapter).buscarCotacao("PETR4");
        verify(twelveDataAdapter, never()).buscarCotacao(any());
    }

    @Test
    @DisplayName("@spec:AC-206 Listar ações retorna página")
    void deveListarAcoesPaginadas() {
        when(repository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(acaoBR)));
        when(mapper.toResponse(acaoBR)).thenReturn(responseBR);

        Page<AcaoResponseDTO> result = service.listar(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("@spec:AC-207 Buscar por ticker existente retorna dados")
    void deveBuscarPorTickerExistente() {
        when(repository.findByTicker("PETR4")).thenReturn(Optional.of(acaoBR));
        when(mapper.toResponse(acaoBR)).thenReturn(responseBR);

        AcaoResponseDTO result = service.buscarPorTicker("PETR4");

        assertThat(result.getTicker()).isEqualTo("PETR4");
    }

    @Test
    @DisplayName("@spec:AC-207 Buscar por ticker inexistente lança não encontrado")
    void deveLancarNotFoundParaTickerInexistente() {
        when(repository.findByTicker("XXXX3")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorTicker("XXXX3"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("@spec:AC-208 Atualizar cotação busca novo preço e atualiza data/hora")
    void deveAtualizarCotacao() {
        CotacaoResultado novaCotacao = new CotacaoResultado(new BigDecimal("42.00"), LocalDateTime.now());
        when(repository.findById(1L)).thenReturn(Optional.of(acaoBR));
        when(brapiAdapter.buscarCotacao("PETR4")).thenReturn(novaCotacao);
        when(repository.save(any())).thenReturn(acaoBR);
        when(mapper.toResponse(acaoBR)).thenReturn(responseBR);

        AcaoResponseDTO result = service.atualizarCotacao(1L);

        assertThat(result).isNotNull();
        verify(brapiAdapter).buscarCotacao("PETR4");
    }

    @Test
    @DisplayName("@spec:AC-209 Cotação sempre acompanha data/hora de obtenção")
    void deveCotacaoAcompanharDataHora() {
        when(repository.existsByTicker("PETR4")).thenReturn(false);
        when(brapiAdapter.buscarCotacao("PETR4")).thenReturn(cotacaoBR);
        when(repository.save(any())).thenReturn(acaoBR);
        when(mapper.toResponse(acaoBR)).thenReturn(responseBR);

        AcaoResponseDTO result = service.cadastrar(new AcaoRequestDTO("PETR4", Mercado.BR));

        assertThat(result.getDataHoraCotacao()).isNotNull();
    }

    @Test
    @DisplayName("@spec:AC-210 Fonte indisponível retorna última cotação conhecida, não falha")
    void deveRetornarUltimaCotacaoQuandoFonteIndisponivel() {
        when(repository.findById(1L)).thenReturn(Optional.of(acaoBR));
        when(brapiAdapter.buscarCotacao("PETR4"))
                .thenThrow(new ExternalServiceException("Fonte BR indisponível"));
        when(mapper.toResponse(acaoBR)).thenReturn(responseBR);

        // Não deve lançar exceção — retorna a última cotação conhecida
        AcaoResponseDTO result = service.atualizarCotacao(1L);

        assertThat(result).isNotNull();
        assertThat(result.getCotacaoAtual()).isEqualByComparingTo("38.00");
    }

    @Test
    @DisplayName("@spec:AC-211 Cota excedida resulta em mensagem específica de limite")
    void deveMensagemEspecificaParaLimiteExcedido() {
        when(repository.existsByTicker("PETR4")).thenReturn(false);
        when(brapiAdapter.buscarCotacao("PETR4"))
                .thenThrow(new ExternalServiceException("Limite de requisições da fonte BR excedido"));

        assertThatThrownBy(() -> service.cadastrar(new AcaoRequestDTO("PETR4", Mercado.BR)))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Limite");
    }
}
