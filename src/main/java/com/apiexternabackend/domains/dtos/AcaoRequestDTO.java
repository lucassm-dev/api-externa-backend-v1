package com.apiexternabackend.domains.dtos;

import com.apiexternabackend.domains.enums.Mercado;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AcaoRequestDTO {

    @NotBlank(message = "Ticker é obrigatório")
    private String ticker;

    @NotNull(message = "Mercado é obrigatório (BR ou US)")
    private Mercado mercado;
}
