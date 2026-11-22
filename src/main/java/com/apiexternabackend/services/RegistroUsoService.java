package com.apiexternabackend.services;

import com.apiexternabackend.domains.RegistroUso;
import com.apiexternabackend.repositories.RegistroUsoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegistroUsoService {

    private final RegistroUsoRepository repository;

    // AC-654: roda no applicationTaskExecutor do Spring Boot, fora da thread da requisição
    // shortcut: a fila do executor padrão não tem limite; trocar por fila limitada se o volume crescer (TDD, seção 6)
    @Async
    public void registrar(RegistroUso registro) {
        repository.save(registro);
    }

    public void descartarAntigos() {
    }
}
