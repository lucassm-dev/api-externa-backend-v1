package com.apiexternabackend.domains.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BloqueioRequestDTO(@NotBlank @Size(max = 255) String motivo) {
}
