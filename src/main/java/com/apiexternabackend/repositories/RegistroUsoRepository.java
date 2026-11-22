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

    @Query(value = "select count(*) from registro_uso where registrado_em >= :inicio and registrado_em < :fim", nativeQuery = true)
    long contarNoPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    // spec-30: date_trunc e limit funcionam no H2 dos testes e no PostgreSQL
    @Query(value = """
            select date_trunc('hour', registrado_em) as hora, count(*) as requisicoes from registro_uso
            where registrado_em >= :inicio and registrado_em < :fim
            group by date_trunc('hour', registrado_em)
            order by requisicoes desc, hora desc
            limit 10""", nativeQuery = true)
    List<Object[]> picosPorHora(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);
}
