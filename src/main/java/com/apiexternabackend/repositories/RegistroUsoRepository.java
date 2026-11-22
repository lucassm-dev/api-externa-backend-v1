package com.apiexternabackend.repositories;

import com.apiexternabackend.domains.RegistroUso;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface RegistroUsoRepository extends Repository<RegistroUso, Long> {

    RegistroUso save(RegistroUso registro);

    long count();

    List<RegistroUso> findByUsuarioId(Long usuarioId);
}
