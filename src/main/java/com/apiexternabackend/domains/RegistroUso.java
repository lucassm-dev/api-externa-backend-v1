package com.apiexternabackend.domains;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

// spec-28 / AC-652: só quem, quando, método, padrão da rota e status
@Entity
@Immutable
@Table(name = "registro_uso")
@Getter
@NoArgsConstructor
public class RegistroUso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 10)
    private String metodo;

    @Column(nullable = false, length = 200)
    private String rota;

    @Column(nullable = false)
    private int status;

    @Column(name = "registrado_em", nullable = false)
    private LocalDateTime registradoEm;

    public RegistroUso(Long usuarioId, String metodo, String rota, int status, LocalDateTime registradoEm) {
        this.usuarioId = usuarioId;
        this.metodo = metodo;
        this.rota = rota;
        this.status = status;
        this.registradoEm = registradoEm;
    }
}
