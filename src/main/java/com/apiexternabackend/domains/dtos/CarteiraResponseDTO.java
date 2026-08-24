package com.apiexternabackend.domains.dtos;

import com.apiexternabackend.domains.enums.Mercado;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CarteiraResponseDTO {

    private Long id;
    private Long investidorId;
    private Long corretoraId;
    private String nomeCorretora;
    private Mercado mercado;
    private String moeda;
    private String nome;
    private Boolean ativa;
}
