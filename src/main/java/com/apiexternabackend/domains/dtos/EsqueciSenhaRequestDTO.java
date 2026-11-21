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
public class EsqueciSenhaRequestDTO {

    @NotBlank(message = "E-mail é obrigatório")
    private String email;
}
