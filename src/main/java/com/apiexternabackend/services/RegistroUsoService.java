package com.apiexternabackend.services;

import com.apiexternabackend.domains.RegistroUso;
import com.apiexternabackend.repositories.RegistroUsoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegistroUsoService {

    private final RegistroUsoRepository repository;

    public void registrar(RegistroUso registro) {
        repository.save(registro);
    }
}
