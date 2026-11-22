package com.apiexternabackend.services;

import com.apiexternabackend.domains.RegistroUso;
import com.apiexternabackend.repositories.RegistroUsoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistroUsoService {

    private static final int DIAS_DE_GUARDA = 90;

    private final RegistroUsoRepository repository;

    // AC-654: roda no applicationTaskExecutor do Spring Boot, fora da thread da requisição
    // shortcut: a fila do executor padrão não tem limite; trocar por fila limitada se o volume crescer (TDD, seção 6)
    @Async
    public void registrar(RegistroUso registro) {
        repository.save(registro);
    }

    // AC-656 / ASM-504: guarda 90 dias; roda às 3h de Brasília, fora do horário de uso
    @Scheduled(cron = "0 0 3 * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void descartarAntigos() {
        int apagados = repository.apagarAnterioresA(LocalDateTime.now().minusDays(DIAS_DE_GUARDA));
        log.info("Descarte do registro de uso: {} registros com mais de {} dias apagados", apagados, DIAS_DE_GUARDA);
    }
}
