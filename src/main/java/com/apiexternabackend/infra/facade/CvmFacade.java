package com.apiexternabackend.infra.facade;

import com.apiexternabackend.config.CvmFeignConfig;
import com.apiexternabackend.infra.client.cvm.CvmCorretoraClient;
import com.apiexternabackend.infra.client.cvm.dto.CvmCorretoraResponseDTO;
import com.apiexternabackend.resources.exceptions.ExternalServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class CvmFacade {

    private static final String SITUACAO_AUTORIZADA = "EM FUNCIONAMENTO NORMAL";

    private final CvmCorretoraClient client;

    public ResultadoVerificacaoCvm verificar(String cnpj) {
        try {
            CvmCorretoraResponseDTO resultado = client.buscarPorCnpj(cnpj);
            String situacao = resultado.getStatus() != null ? resultado.getStatus().trim() : "";
            if (!SITUACAO_AUTORIZADA.equalsIgnoreCase(situacao)) {
                return ResultadoVerificacaoCvm.naoAutorizada(LocalDate.now(),
                        "Corretora não autorizada na CVM: situação = " + situacao);
            }
            return ResultadoVerificacaoCvm.autorizada(LocalDate.now());
        } catch (ExternalServiceException e) {
            if (CvmFeignConfig.CNPJ_NAO_ENCONTRADO.equals(e.getMessage())) {
                return ResultadoVerificacaoCvm.naoAutorizada(LocalDate.now(),
                        "Corretora não autorizada na CVM: CNPJ não consta na base de participantes");
            }
            return ResultadoVerificacaoCvm.falhaVerificacao(null,
                    "Não foi possível verificar a autorização na CVM: " + e.getMessage());
        } catch (Exception e) {
            return ResultadoVerificacaoCvm.falhaVerificacao(null,
                    "Não foi possível verificar a autorização na CVM: " + e.getMessage());
        }
    }

    public record ResultadoVerificacaoCvm(
            boolean autorizada,
            boolean falhaVerificacao,
            LocalDate dataBase,
            String mensagem
    ) {
        public static ResultadoVerificacaoCvm autorizada(LocalDate dataBase) {
            return new ResultadoVerificacaoCvm(true, false, dataBase, null);
        }

        public static ResultadoVerificacaoCvm naoAutorizada(LocalDate dataBase, String mensagem) {
            return new ResultadoVerificacaoCvm(false, false, dataBase, mensagem);
        }

        public static ResultadoVerificacaoCvm falhaVerificacao(LocalDate dataBase, String mensagem) {
            return new ResultadoVerificacaoCvm(false, true, dataBase, mensagem);
        }
    }
}