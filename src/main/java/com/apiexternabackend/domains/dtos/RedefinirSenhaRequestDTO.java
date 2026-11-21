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
public class RedefinirSenhaRequestDTO {

    @NotBlank(message = "E-mail é obrigatório")
    private String email;

    @NotBlank(message = "Código é obrigatório")
    private String codigo;

    @NotBlank(message = "Nova senha é obrigatória")
    private String novaSenha;
}
