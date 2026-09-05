package com.apiexternabackend.domains.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemBarraCotacoesDTO {

    private String simbolo;
    private String nome;
    private BigDecimal preco;
    private BigDecimal variacaoPercentual;
    private String logoUrl;
}