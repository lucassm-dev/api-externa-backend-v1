package com.apiexternabackend.domains;

import com.apiexternabackend.domains.enums.Mercado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "carteira")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Carteira {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investidor_id", nullable = false)
    private Investidor investidor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corretora_id", nullable = false)
    private Corretora corretora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private Mercado mercado;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private Boolean ativa = true;
}
