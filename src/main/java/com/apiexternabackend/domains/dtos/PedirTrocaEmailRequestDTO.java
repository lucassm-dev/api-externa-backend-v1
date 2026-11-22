package com.apiexternabackend.domains.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PedirTrocaEmailRequestDTO {

    @NotBlank(message = "Novo e-mail é obrigatório")
    @Email(message = "E-mail inválido")
    private String novoEmail;

    @NotBlank(message = "Senha atual é obrigatória")
    private String senhaAtual;
}
