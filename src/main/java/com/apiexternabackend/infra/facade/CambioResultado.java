package com.apiexternabackend.infra.facade;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CambioResultado(BigDecimal taxa, LocalDateTime dataHora) {
}
