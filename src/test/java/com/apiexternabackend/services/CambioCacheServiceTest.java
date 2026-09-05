package com.apiexternabackend.services;

import com.apiexternabackend.infra.facade.CambioFacade;
import com.apiexternabackend.infra.facade.CambioResultado;
import com.apiexternabackend.resources.exceptions.IntegracaoExternaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CambioCacheServiceTest {

    @Mock private CambioFacade facade;

    private CambioCacheService service(long ttlMinutos) {
        return new CambioCacheService(facade, ttlMinutos);
    }

    @Test
    @DisplayName("@spec:AC-491 Taxa dentro do TTL é reaproveitada sem chamar a fonte")
    void deveReaproveitarTaxaDentroDoTtl() {
        CambioCacheService service = service(15);
        when(facade.buscarTaxaUsdBrl()).thenReturn(new CambioResultado(new BigDecimal("5.30"), LocalDateTime.now()));

        service.obterTaxaAtual(); // primeira chamada busca e guarda em cache
        CambioCacheService.CambioObtido segunda = service.obterTaxaAtual(); // dentro do TTL

        assertThat(segunda.desatualizado()).isFalse();
        verify(facade, times(1)).buscarTaxaUsdBrl();
    }

    @Test
    @DisplayName("@spec:AC-492 Taxa fora do TTL busca de novo")
    void deveBuscarDeNovoForaDoTtl() {
        CambioCacheService service = service(15);
        when(facade.buscarTaxaUsdBrl())
                .thenReturn(new CambioResultado(new BigDecimal("5.30"), LocalDateTime.now().minusMinutes(20)))
                .thenReturn(new CambioResultado(new BigDecimal("5.35"), LocalDateTime.now()));

        service.obterTaxaAtual();
        CambioCacheService.CambioObtido segunda = service.obterTaxaAtual();

        assertThat(segunda.resultado().taxa()).isEqualByComparingTo("5.35");
        verify(facade, times(2)).buscarTaxaUsdBrl();
    }

    @Test
    @DisplayName("@spec:AC-489 Fonte falha mas há cache: devolve a última taxa conhecida, marcada como desatualizada")
    void deveDevolverUltimaTaxaConhecidaQuandoFonteFalha() {
        CambioCacheService service = service(15);
        when(facade.buscarTaxaUsdBrl())
                .thenReturn(new CambioResultado(new BigDecimal("5.30"), LocalDateTime.now().minusMinutes(20)))
                .thenThrow(new IntegracaoExternaException("EXT-011", "Fontes de câmbio indisponíveis", false));

        service.obterTaxaAtual();
        CambioCacheService.CambioObtido segunda = service.obterTaxaAtual();

        assertThat(segunda.desatualizado()).isTrue();
        assertThat(segunda.resultado().taxa()).isEqualByComparingTo("5.30");
    }

    @Test
    @DisplayName("@spec:AC-490 Fonte falha sem cache: lança exceção, sem fallback possível")
    void deveLancarExcecaoSemCacheEFonteFalhando() {
        CambioCacheService service = service(15);
        when(facade.buscarTaxaUsdBrl())
                .thenThrow(new IntegracaoExternaException("EXT-011", "Fontes de câmbio indisponíveis", false));

        assertThatThrownBy(service::obterTaxaAtual)
                .isInstanceOf(IntegracaoExternaException.class);
    }
}