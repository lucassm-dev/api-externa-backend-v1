package com.apiexternabackend.domains;

import com.apiexternabackend.domains.enums.Papel;
import com.apiexternabackend.domains.enums.SituacaoUsuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // sem unique = true: a unicidade é parcial (só entre não excluídos, índice da migration V17)
    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Papel papel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoUsuario situacao;

    @Column(name = "motivo_bloqueio")
    private String motivoBloqueio;

    @Column(name = "bloqueado_em")
    private LocalDateTime bloqueadoEm;

    public Usuario(Long id, String email, String senha, Papel papel, SituacaoUsuario situacao) {
        this.id = id;
        this.email = email;
        this.senha = senha;
        this.papel = papel;
        this.situacao = situacao;
    }
}
