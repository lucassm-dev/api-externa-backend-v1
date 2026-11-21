package com.apiexternabackend.domains.dtos;

import com.apiexternabackend.domains.enums.Papel;

// spec-17: nome é null para quem não tem perfil de investidor (ADMIN)
public record UsuarioLogadoResponseDTO(Long id, String nome, String email, Papel papel) {
}
