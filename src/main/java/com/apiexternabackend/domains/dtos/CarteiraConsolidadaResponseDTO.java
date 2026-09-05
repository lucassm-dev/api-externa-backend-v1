package com.apiexternabackend.domains.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CarteiraConsolidadaResponseDTO {

    private BigDecimal valorInvestido;
    private BigDecimal valorDeMercado;
    private BigDecimal lucroNaoRealizado;
    private BigDecimal taxaCambioAtual;
    private LocalDateTime dataHoraTaxaCambio;
    private List<String> avisos = new ArrayList<>();
}