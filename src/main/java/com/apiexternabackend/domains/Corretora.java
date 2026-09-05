package com.apiexternabackend.domains;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "corretora")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Corretora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 14)
    private String cnpj;

    @Column(nullable = false)
    private String razaoSocial;

    private String nomeFantasia;
    private String email;
    private String telefone;

    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;

    @Column(length = 2)
    private String uf;

    private String situacaoCadastral;

    @Column(nullable = false)
    private Boolean validadaNaCvm = false;

    @Column(nullable = false)
    private Boolean ativo = true;

    private LocalDate dataBaseCvm;

    @Column(nullable = false)
    private LocalDateTime dataCadastro = LocalDateTime.now();
}
