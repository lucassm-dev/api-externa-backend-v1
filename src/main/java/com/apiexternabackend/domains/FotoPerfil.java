package com.apiexternabackend.domains;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "foto_perfil")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FotoPerfil {

    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    // length só pesa no H2 dos testes (varbinary); no Postgres a coluna é bytea
    @Column(nullable = false, length = 10 * 1024 * 1024)
    private byte[] conteudo;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(name = "atualizada_em", nullable = false)
    private Instant atualizadaEm;
}
