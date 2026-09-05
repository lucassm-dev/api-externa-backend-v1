package com.apiexternabackend.services;

import com.apiexternabackend.domains.Acao;
import com.apiexternabackend.domains.enums.Mercado;
import com.apiexternabackend.infra.adapter.BrapiAdapter;
import com.apiexternabackend.infra.adapter.CotacaoResultado;
import com.apiexternabackend.infra.adapter.TwelveDataAdapter;
import com.apiexternabackend.repositories.AcaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CotacaoCacheServiceTest {

    @Mock private AcaoRepository acaoRepository;
    @Mock private BrapiAdapter brapiAdapter;
    @Mock private TwelveDataAdapter twelveDataAdapter;

    private Acao acao;

    @BeforeEach
    void setUp() {
        acao = new Acao(1L, "PETR4", "Petrobras", Mercado.BR, "BRL", new BigDecimal("38"), null, true);
    }

    private CotacaoCacheService service(long ttlMinutos) {
        return new CotacaoCacheService(acaoRepository, brapiAdapter, twelveDataAdapter, ttlMinutos);
    }

    @Test
    @DisplayName("@spec:AC-477 Cotação dentro do TTL é reaproveitada sem chamar a fonte")
    void deveReaproveitarCotacaoDentroDoTtl() {
        acao.setDataHoraCotacao(LocalDateTime.now().minusMinutes(5));
        CotacaoCacheService service = service(15);

        CotacaoResultado resultado = service.obter(acao, false);

        assertThat(resultado.preco()).isEqualByComparingTo("38");
        verify(brapiAdapter, never()).buscarCotacao(any());
        verify(acaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("@spec:AC-478 Cotação fora do TTL busca na fonte e atualiza o cache")
    void deveBuscarNaFonteQuandoForaDoTtl() {
        acao.setDataHoraCotacao(LocalDateTime.now().minusMinutes(20));
        CotacaoResultado fresca = new CotacaoResultado(new BigDecimal("42.00"), LocalDateTime.now());
        when(brapiAdapter.buscarCotacao("PETR4")).thenReturn(fresca);
        CotacaoCacheService service = service(15);

        CotacaoResultado resultado = service.obter(acao, false);

        assertThat(resultado.preco()).isEqualByComparingTo("42.00");
        assertThat(acao.getCotacaoAtual()).isEqualByComparingTo("42.00");
        verify(acaoRepository).save(acao);
    }

    @Test
    @DisplayName("@spec:AC-478 Nunca ter cotação salva também busca na fonte")
    void deveBuscarNaFonteQuandoNuncaTeveCotacao() {
        acao.setDataHoraCotacao(null);
        CotacaoResultado fresca = new CotacaoResultado(new BigDecimal("42.00"), LocalDateTime.now());
        when(brapiAdapter.buscarCotacao("PETR4")).thenReturn(fresca);
        CotacaoCacheService service = service(15);

        service.obter(acao, false);

        verify(brapiAdapter).buscarCotacao("PETR4");
    }

    @Test
    @DisplayName("@spec:AC-481 Forçar atualização ignora o cache mesmo dentro do TTL")
    void deveForcarBuscaNaFonteIgnorandoTtl() {
        acao.setDataHoraCotacao(LocalDateTime.now().minusMinutes(1));
        CotacaoResultado fresca = new CotacaoResultado(new BigDecimal("42.00"), LocalDateTime.now());
        when(brapiAdapter.buscarCotacao("PETR4")).thenReturn(fresca);
        CotacaoCacheService service = service(15);

        service.obter(acao, true);

        verify(brapiAdapter).buscarCotacao("PETR4");
    }

    @Test
    @DisplayName("@spec:AC-480 TTL configurável muda o comportamento de cache")
    void deveRespeitarTtlConfigurado() {
        acao.setDataHoraCotacao(LocalDateTime.now().minusMinutes(10));
        CotacaoCacheService serviceTtlCurto = service(5); // 10 min > TTL de 5 min → busca na fonte
        when(brapiAdapter.buscarCotacao("PETR4"))
                .thenReturn(new CotacaoResultado(new BigDecimal("42.00"), LocalDateTime.now()));

        serviceTtlCurto.obter(acao, false);

        verify(brapiAdapter).buscarCotacao("PETR4");
    }
}