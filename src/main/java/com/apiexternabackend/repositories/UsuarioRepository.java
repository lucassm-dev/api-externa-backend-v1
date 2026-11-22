package com.apiexternabackend.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.apiexternabackend.domains.Usuario;
import com.apiexternabackend.domains.enums.Papel;
import com.apiexternabackend.domains.enums.SituacaoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByEmailAndSituacaoNot(String email, SituacaoUsuario situacao);

    boolean existsByPapelAndSituacaoNot(Papel papel, SituacaoUsuario situacao);

    Optional<Usuario> findByEmailAndSituacaoNot(String email, SituacaoUsuario situacao);

    // spec-18/spec-29: excluídos nunca aparecem; situação e papel são filtros opcionais
    @Query("""
            select u from Usuario u
            where u.situacao <> com.apiexternabackend.domains.enums.SituacaoUsuario.EXCLUIDO
              and (:situacao is null or u.situacao = :situacao)
              and (:papel is null or u.papel = :papel)""")
    Page<Usuario> listar(@Param("situacao") SituacaoUsuario situacao, @Param("papel") Papel papel, Pageable pageable);
}
