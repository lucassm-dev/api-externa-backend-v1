package com.apiexternabackend.repositories;

import com.apiexternabackend.domains.CodigoRecuperacao;
import com.apiexternabackend.domains.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CodigoRecuperacaoRepository extends JpaRepository<CodigoRecuperacao, Long> {

    long countByUsuarioAndCriadoEmAfter(Usuario usuario, Instant desde);

    List<CodigoRecuperacao> findByUsuarioAndUsadoEmIsNull(Usuario usuario);

    Optional<CodigoRecuperacao> findFirstByUsuarioAndUsadoEmIsNullOrderByCriadoEmDesc(Usuario usuario);
}
