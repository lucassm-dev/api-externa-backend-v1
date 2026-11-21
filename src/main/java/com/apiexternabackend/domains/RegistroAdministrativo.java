package com.apiexternabackend.domains;

import com.apiexternabackend.domains.enums.AcaoAdministrativa;
import com.apiexternabackend.domains.enums.AlvoAdministrativo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

// ADR-013 / spec-19: só ids, sem senha nem token; gravado uma vez e nunca alterado (ASM-493)
@Entity
@Immutable
@Table(name = "registro_administrativo")
@Getter
@NoArgsConstructor
public class RegistroAdministrativo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AcaoAdministrativa acao;

    @Enumerated(EnumType.STRING)
    @Column(name = "alvo_tipo", nullable = false, length = 20)
    private AlvoAdministrativo alvoTipo;

    @Column(name = "alvo_id", nullable = false)
    private Long alvoId;

    private String motivo;

    @Column(name = "registrado_em", nullable = false)
    private LocalDateTime registradoEm;

    public RegistroAdministrativo(Long adminId, AcaoAdministrativa acao, AlvoAdministrativo alvoTipo, Long alvoId, String motivo) {
        this.adminId = adminId;
        this.acao = acao;
        this.alvoTipo = alvoTipo;
        this.alvoId = alvoId;
        this.motivo = motivo;
        this.registradoEm = LocalDateTime.now();
    }
}
