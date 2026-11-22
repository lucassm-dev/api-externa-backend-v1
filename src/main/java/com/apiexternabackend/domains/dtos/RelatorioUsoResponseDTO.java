package com.apiexternabackend.domains.dtos;

import java.time.LocalDateTime;
import java.util.List;

// spec-30: só contagens; nada de rota, carteira ou valor
public record RelatorioUsoResponseDTO(long total, List<Pico> picos) {

    public record Pico(LocalDateTime hora, long requisicoes) {
    }
}
