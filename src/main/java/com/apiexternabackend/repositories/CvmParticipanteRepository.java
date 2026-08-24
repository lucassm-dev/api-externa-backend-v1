package com.apiexternabackend.repositories;

import com.apiexternabackend.domains.CvmParticipante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Optional;

public interface CvmParticipanteRepository extends JpaRepository<CvmParticipante, String> {

    Optional<CvmParticipante> findByCnpj(String cnpj);

    @Query("SELECT MAX(c.dataBase) FROM CvmParticipante c")
    Optional<LocalDate> findDataBaseMaisRecente();

    long countByDataBase(LocalDate dataBase);
}
