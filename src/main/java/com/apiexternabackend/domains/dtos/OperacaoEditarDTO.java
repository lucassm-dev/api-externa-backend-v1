package com.apiexternabackend.domains.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OperacaoEditarDTO {

    @Min(value = 1, message = "Quantidade mínima é 1")
    private Integer quantidade;

    @Positive(message = "Preço unitário deve ser maior que zero")
    private BigDecimal precoUnitario;
}
