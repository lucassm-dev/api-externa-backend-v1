package com.apiexternabackend.infra.client.cvm.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class CvmCorretoraResponseDTO {

    private String cnpj;
    private String nomeEmpresa;
    private String tipoParticipante;
    private String situacao;
    private LocalDate dataBase;
}
