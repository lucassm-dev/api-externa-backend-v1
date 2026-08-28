package com.apiexternabackend.infra.facade;

import com.apiexternabackend.config.CvmFeignConfig;
import com.apiexternabackend.infra.client.cvm.CvmCorretoraClient;
import com.apiexternabackend.infra.client.cvm.dto.CvmCorretoraResponseDTO;
import com.apiexternabackend.infra.facade.CvmFacade.ResultadoVerificacaoCvm;
import com.apiexternabackend.resources.exceptions.ExternalServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CvmFacadeTest {

    private static final String CNPJ = "02332886000104";

    @Mock
    private CvmCorretoraClient client;

    @InjectMocks
    private CvmFacade facade;

    @Test
    @DisplayName("@spec:AC-105 CNPJ em funcionamento normal retorna resultado autorizado")
    void deveRetornarAutorizadoQuandoSituacaoEmFuncionamentoNormal() {
        CvmCorretoraResponseDTO dto = new CvmCorretoraResponseDTO();
        dto.setStatus("EM FUNCIONAMENTO NORMAL");
        when(client.buscarPorCnpj(CNPJ)).thenReturn(dto);

        ResultadoVerificacaoCvm result = facade.verificar(CNPJ);

        assertThat(result.autorizada()).isTrue();
        assertThat(result.dataBase()).isNotNull();
    }

    @Test
    @DisplayName("@spec:AC-105 CNPJ com situação diferente de EM FUNCIONAMENTO NORMAL é não-autorizado")
    void deveRetornarNaoAutorizadoQuandoSituacaoCancelada() {
        CvmCorretoraResponseDTO dto = new CvmCorretoraResponseDTO();
        dto.setStatus("CANCELADA");
        when(client.buscarPorCnpj(CNPJ)).thenReturn(dto);

        ResultadoVerificacaoCvm result = facade.verificar(CNPJ);

        assertThat(result.autorizada()).isFalse();
        assertThat(result.falhaVerificacao()).isFalse();
    }

    @Test
    @DisplayName("@spec:AC-105 CNPJ não encontrado na CVM é não-autorizado")
    void deveRetornarNaoAutorizadoQuandoCnpjNaoConsta() {
        when(client.buscarPorCnpj(CNPJ))
                .thenThrow(new ExternalServiceException(CvmFeignConfig.CNPJ_NAO_ENCONTRADO));

        ResultadoVerificacaoCvm result = facade.verificar(CNPJ);

        assertThat(result.autorizada()).isFalse();
        assertThat(result.falhaVerificacao()).isFalse();
    }

    @Test
    @DisplayName("@spec:AC-106 Falha na chamada CVM resulta em falha de verificação, não reprovação")
    void deveRetornarFalhaVerificacaoQuandoCvmIndisponivel() {
        when(client.buscarPorCnpj(CNPJ))
                .thenThrow(new ExternalServiceException("Erro ao consultar CVM: HTTP 500"));

        ResultadoVerificacaoCvm result = facade.verificar(CNPJ);

        assertThat(result.falhaVerificacao()).isTrue();
        assertThat(result.mensagem()).contains("verificar");
        assertThat(result.mensagem()).doesNotContain("não autorizada");
    }
}