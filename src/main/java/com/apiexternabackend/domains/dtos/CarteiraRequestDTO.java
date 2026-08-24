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
public class CarteiraRequestDTO {

    @NotNull(message = "ID do investidor é obrigatório")
    private Long investidorId;

    @NotNull(message = "ID da corretora é obrigatório")
    private Long corretoraId;

    @NotNull(message = "Mercado é obrigatório (BR ou US)")
    private Mercado mercado;

    @NotBlank(message = "Nome da carteira é obrigatório")
    private String nome;
}
