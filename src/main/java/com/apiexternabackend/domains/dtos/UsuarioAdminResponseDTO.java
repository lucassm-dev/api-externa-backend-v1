package com.apiexternabackend.domains.dtos;

import com.apiexternabackend.domains.enums.Papel;
import com.apiexternabackend.domains.enums.SituacaoUsuario;

// ADR-013: o ADMIN vê só indicadores, nunca o conteúdo das carteiras nem a senha
public record UsuarioAdminResponseDTO(Long id, String email, String nome, Papel papel, SituacaoUsuario situacao,
                                      String motivoBloqueio, long carteirasAtivas, boolean temPosicaoAberta) {
}
