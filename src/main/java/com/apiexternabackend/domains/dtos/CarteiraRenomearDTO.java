package com.apiexternabackend.domains.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CarteiraRenomearDTO {

    @NotBlank(message = "Nome é obrigatório")
    private String nome;
}
