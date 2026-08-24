package com.apiexternabackend.domains.dtos;

import com.apiexternabackend.domains.enums.TipoOperacao;
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
public class OperacaoResponseDTO {

    private Long id;
    private Long carteiraId;
    private String ticker;
    private TipoOperacao tipo;
    private Integer quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal valorTotal;
    private LocalDateTime dataHora;
}
