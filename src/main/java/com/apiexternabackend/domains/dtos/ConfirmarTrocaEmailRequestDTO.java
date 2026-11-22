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
public class ConfirmarTrocaEmailRequestDTO {

    @NotBlank(message = "Código é obrigatório")
    private String codigo;
}
