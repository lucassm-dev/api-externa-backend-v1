package com.apiexternabackend.domains.dtos;

import com.apiexternabackend.domains.enums.Mercado;
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
public class AcaoResponseDTO {

    private Long id;
    private String ticker;
    private String nomeEmpresa;
    private Mercado mercado;
    private String moeda;
    private BigDecimal cotacaoAtual;
    private LocalDateTime dataHoraCotacao;
}
