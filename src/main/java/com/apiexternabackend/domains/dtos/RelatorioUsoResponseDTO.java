package com.apiexternabackend.domains.dtos;

import java.time.LocalDateTime;
import java.util.List;

// spec-30: contagens e quem fez (ASM-511); nada de rota, carteira ou valor
public record RelatorioUsoResponseDTO(long total, List<Pico> picos, List<UsuarioUso> usuarios) {

    public record Pico(LocalDateTime hora, long requisicoes) {
    }

    public record UsuarioUso(Long id, String email, long requisicoes) {
    }
}
