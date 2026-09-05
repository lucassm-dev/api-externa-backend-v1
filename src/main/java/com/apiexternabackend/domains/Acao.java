package com.apiexternabackend.domains;

import com.apiexternabackend.domains.enums.Mercado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "acao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Acao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ticker;

    private String nomeEmpresa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private Mercado mercado;

    @Column(nullable = false, length = 3)
    private String moeda;

    private BigDecimal cotacaoAtual;

    private LocalDateTime dataHoraCotacao;

    @Column(nullable = false)
    private Boolean ativo = true;
}
