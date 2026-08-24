package com.apiexternabackend.domains;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "cvm_participante")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CvmParticipante {

    @Id
    @Column(length = 14)
    private String cnpj;

    @Column(nullable = false)
    private String nomeEmpresa;

    @Column(nullable = false)
    private String tipoParticipante;

    @Column(nullable = false)
    private String situacao;

    @Column(nullable = false)
    private LocalDate dataBase;
}
