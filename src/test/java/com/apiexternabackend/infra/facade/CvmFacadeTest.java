package com.apiexternabackend.infra.facade;

import com.apiexternabackend.domains.CvmParticipante;
import com.apiexternabackend.infra.facade.CvmFacade.ResultadoVerificacaoCvm;
import com.apiexternabackend.repositories.CvmParticipanteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CvmFacadeTest {

    private static final String CNPJ = "02332886000104";
    private static final LocalDate DATA_BASE = LocalDate.now();

    @Mock
    private CvmParticipanteRepository repository;

    @InjectMocks
    private CvmFacade facade;

    @Test
    @DisplayName("@spec:AC-105 CNPJ autorizado na CVM retorna resultado autorizado com data da base")
    void deveRetornarAutorizadoQuandoSituacaoAutorizado() {
        CvmParticipante p = new CvmParticipante(CNPJ, "Corretora X", "CORRETORA", "AUTORIZADO", DATA_BASE);
        when(repository.findDataBaseMaisRecente()).thenReturn(Optional.of(DATA_BASE));
        when(repository.findByCnpj(CNPJ)).thenReturn(Optional.of(p));

        ResultadoVerificacaoCvm result = facade.verificar(CNPJ);

        assertThat(result.autorizada()).isTrue();
        assertThat(result.dataBase()).isEqualTo(DATA_BASE);
    }

    @Test
    @DisplayName("@spec:AC-105 CNPJ com situação diferente de AUTORIZADO é não-autorizado")
    void deveRetornarNaoAutorizadoQuandoSituacaoCancelado() {
        CvmParticipante p = new CvmParticipante(CNPJ, "Corretora X", "CORRETORA", "CANCELADO", DATA_BASE);
        when(repository.findDataBaseMaisRecente()).thenReturn(Optional.of(DATA_BASE));
        when(repository.findByCnpj(CNPJ)).thenReturn(Optional.of(p));

        ResultadoVerificacaoCvm result = facade.verificar(CNPJ);

        assertThat(result.autorizada()).isFalse();
        assertThat(result.falhaVerificacao()).isFalse();
    }

    @Test
    @DisplayName("@spec:AC-105 CNPJ que não consta na CVM é não-autorizado")
    void deveRetornarNaoAutorizadoQuandoCnpjNaoConsta() {
        when(repository.findDataBaseMaisRecente()).thenReturn(Optional.of(DATA_BASE));
        when(repository.findByCnpj(CNPJ)).thenReturn(Optional.empty());

        ResultadoVerificacaoCvm result = facade.verificar(CNPJ);

        assertThat(result.autorizada()).isFalse();
        assertThat(result.falhaVerificacao()).isFalse();
    }

    @Test
    @DisplayName("@spec:AC-106 Base CVM vazia resulta em falha de verificação, não reprovação")
    void deveRetornarFalhaVerificacaoQuandoBaseVazia() {
        when(repository.findDataBaseMaisRecente()).thenReturn(Optional.empty());

        ResultadoVerificacaoCvm result = facade.verificar(CNPJ);

        assertThat(result.falhaVerificacao()).isTrue();
        assertThat(result.mensagem()).contains("verificar");
        assertThat(result.mensagem()).doesNotContain("não autorizada");
    }
}
