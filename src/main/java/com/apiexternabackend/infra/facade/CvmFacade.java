package com.apiexternabackend.infra.facade;

import com.apiexternabackend.domains.CvmParticipante;
import com.apiexternabackend.repositories.CvmParticipanteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Verifica autorização de corretora na base local da CVM.
 * Distingue "não autorizada" (consta como irregular) de
 * "falha na verificação" (base vazia/ausente — RN-C04).
 */
@Component
@RequiredArgsConstructor
public class CvmFacade {

    private static final String SITUACAO_AUTORIZADA = "AUTORIZADO";

    private final CvmParticipanteRepository repository;

    public ResultadoVerificacaoCvm verificar(String cnpj) {
        Optional<LocalDate> dataBase = repository.findDataBaseMaisRecente();

        if (dataBase.isEmpty()) {
            return ResultadoVerificacaoCvm.falhaVerificacao(null,
                    "Não foi possível verificar a autorização na CVM: base de dados indisponível");
        }

        Optional<CvmParticipante> participante = repository.findByCnpj(cnpj);

        if (participante.isEmpty()) {
            return ResultadoVerificacaoCvm.naoAutorizada(dataBase.get(),
                    "Corretora não autorizada na CVM: CNPJ não consta na base de participantes");
        }

        boolean autorizada = SITUACAO_AUTORIZADA.equalsIgnoreCase(participante.get().getSituacao().trim());

        if (!autorizada) {
            return ResultadoVerificacaoCvm.naoAutorizada(dataBase.get(),
                    "Corretora não autorizada na CVM: situação = " + participante.get().getSituacao());
        }

        return ResultadoVerificacaoCvm.autorizada(dataBase.get());
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
