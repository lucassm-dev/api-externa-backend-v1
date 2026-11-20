package com.apiexternabackend.domains;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "investidor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Investidor {

    // spec-15: o id é o do usuario (@MapsId) — o sub do token continua sendo o id do investidor
    @Id
    private Long id;

    @MapsId
    @OneToOne(optional = false)
    @JoinColumn(name = "id")
    private Usuario usuario;

    @Column(nullable = false)
    private String nome;

    // sem unique = true: a unicidade é parcial (só entre ativos, índice da migration V16).
    // Deixar aqui recriaria uma constraint incondicional no schema de teste (ASM-440).
    @Column(nullable = false)
    private String cpf;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(nullable = false)
    private Boolean ativo = true;
}
