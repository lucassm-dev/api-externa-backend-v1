package com.apiexternabackend.repositories;

import com.apiexternabackend.domains.RegistroAdministrativo;
import org.springframework.data.repository.Repository;

import java.util.List;

// AC-591: Repository em vez de JpaRepository — sem delete, saveAll nem update
public interface RegistroAdministrativoRepository extends Repository<RegistroAdministrativo, Long> {

    RegistroAdministrativo save(RegistroAdministrativo registro);

    long count();

    List<RegistroAdministrativo> findByAlvoId(Long alvoId);
}
