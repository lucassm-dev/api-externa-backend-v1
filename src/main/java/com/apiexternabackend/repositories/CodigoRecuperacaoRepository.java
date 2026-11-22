package com.apiexternabackend.repositories;

import com.apiexternabackend.domains.CodigoRecuperacao;
import com.apiexternabackend.domains.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CodigoRecuperacaoRepository extends JpaRepository<CodigoRecuperacao, Long> {

    long countByUsuarioAndNovoEmailIsNullAndCriadoEmAfter(Usuario usuario, Instant desde);

    List<CodigoRecuperacao> findByUsuarioAndNovoEmailIsNullAndUsadoEmIsNull(Usuario usuario);

    Optional<CodigoRecuperacao> findFirstByUsuarioAndNovoEmailIsNullAndUsadoEmIsNullOrderByCriadoEmDesc(Usuario usuario);

    long countByUsuarioAndNovoEmailIsNotNullAndCriadoEmAfter(Usuario usuario, Instant desde);

    List<CodigoRecuperacao> findByUsuarioAndNovoEmailIsNotNullAndUsadoEmIsNull(Usuario usuario);

    Optional<CodigoRecuperacao> findFirstByUsuarioAndNovoEmailIsNotNullAndUsadoEmIsNullOrderByCriadoEmDesc(Usuario usuario);
}
