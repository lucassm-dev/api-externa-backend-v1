package com.apiexternabackend.domains.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CarteiraAcaoResponseDTO {

    private Long id;
    private String ticker;
    private String nomeEmpresa;
    private Integer quantidade;
    private BigDecimal precoMedio;
    private BigDecimal cotacaoAtual;
    private LocalDateTime dataHoraCotacao;
    private BigDecimal rentabilidadeNaoRealizada;
}
