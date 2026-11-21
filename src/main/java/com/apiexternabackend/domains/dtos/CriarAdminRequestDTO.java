package com.apiexternabackend.domains.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CriarAdminRequestDTO(@NotBlank @Email String email, @NotBlank String senha) {
}
