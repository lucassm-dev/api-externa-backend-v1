package com.apiexternabackend.infra.adapter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CotacaoResultado(
        BigDecimal preco,
        LocalDateTime dataHora
) {
}
