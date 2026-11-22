package com.apiexternabackend.repositories;

import com.apiexternabackend.domains.RegistroUso;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RegistroUsoRepository extends Repository<RegistroUso, Long> {

    RegistroUso save(RegistroUso registro);

    long count();

    List<RegistroUso> findByUsuarioId(Long usuarioId);

    @Modifying
    @Query("delete from RegistroUso r where r.registradoEm < :limite")
    int apagarAnterioresA(@Param("limite") LocalDateTime limite);
}
