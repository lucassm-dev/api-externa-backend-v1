package com.apiexternabackend.domains.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CorretoraRequestDTO {

    @NotBlank(message = "CNPJ é obrigatório")
    @Pattern(
        regexp = "\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}|\\d{14}",
        message = "CNPJ deve estar no formato 00.000.000/0000-00 ou 14 dígitos"
    )
    private String cnpj;
}
