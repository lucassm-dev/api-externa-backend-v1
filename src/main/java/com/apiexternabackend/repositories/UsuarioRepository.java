package com.apiexternabackend.repositories;

import com.apiexternabackend.domains.Usuario;
import com.apiexternabackend.domains.enums.Papel;
import com.apiexternabackend.domains.enums.SituacaoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByEmailAndSituacaoNot(String email, SituacaoUsuario situacao);

    boolean existsByPapelAndSituacaoNot(Papel papel, SituacaoUsuario situacao);

    Optional<Usuario> findByEmailAndSituacaoNot(String email, SituacaoUsuario situacao);
}
