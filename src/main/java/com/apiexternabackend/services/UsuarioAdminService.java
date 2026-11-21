package com.apiexternabackend.services;

import com.apiexternabackend.domains.Investidor;
import com.apiexternabackend.domains.Usuario;
import com.apiexternabackend.domains.dtos.CriarAdminRequestDTO;
import com.apiexternabackend.domains.dtos.UsuarioAdminResponseDTO;
import com.apiexternabackend.domains.enums.Papel;
import com.apiexternabackend.domains.enums.SituacaoUsuario;
import com.apiexternabackend.repositories.CarteiraAcaoRepository;
import com.apiexternabackend.repositories.CarteiraRepository;
import com.apiexternabackend.repositories.InvestidorRepository;
import com.apiexternabackend.repositories.UsuarioRepository;
import com.apiexternabackend.resources.exceptions.RecursoDuplicadoException;
import com.apiexternabackend.resources.exceptions.RecursoNaoEncontradoException;
import com.apiexternabackend.resources.exceptions.RegraVioladaException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UsuarioAdminService {

    private final UsuarioRepository usuarioRepository;
    private final InvestidorRepository investidorRepository;
    private final CarteiraRepository carteiraRepository;
    private final CarteiraAcaoRepository carteiraAcaoRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<UsuarioAdminResponseDTO> listar(SituacaoUsuario situacao, Pageable pageable) {
        if (situacao == SituacaoUsuario.EXCLUIDO) {
            return Page.empty(pageable);
        }
        Page<Usuario> usuarios = situacao == null
                ? usuarioRepository.findBySituacaoNot(SituacaoUsuario.EXCLUIDO, pageable)
                : usuarioRepository.findBySituacao(situacao, pageable);
        // shortcut: 3 consultas por usuário da página (N+1); trocar por agregação se a página crescer
        return usuarios.map(this::toResponse);
    }

    @Transactional
    public UsuarioAdminResponseDTO criarAdmin(CriarAdminRequestDTO dto) {
        if (usuarioRepository.existsByEmailAndSituacaoNot(dto.email(), SituacaoUsuario.EXCLUIDO)) {
            throw new RecursoDuplicadoException("AUT-001", "E-mail já está em uso: " + dto.email());
        }
        if (!AutenticacaoService.POLITICA_SENHA.matcher(dto.senha()).matches()) {
            throw new RegraVioladaException("AUT-008",
                    "Senha deve ter no mínimo 8 caracteres, com pelo menos uma letra e um número.");
        }
        Usuario admin = usuarioRepository.save(new Usuario(null, dto.email(),
                passwordEncoder.encode(dto.senha()), Papel.ADMIN, SituacaoUsuario.ATIVO));
        return toResponse(admin);
    }

    @Transactional
    public UsuarioAdminResponseDTO bloquear(Long id, String motivo, Long adminId) {
        if (id.equals(adminId)) {
            throw new RegraVioladaException("AUT-011", "Um ADMIN não pode bloquear a si mesmo.");
        }
        Usuario usuario = buscar(id);
        usuario.setSituacao(SituacaoUsuario.BLOQUEADO);
        usuario.setMotivoBloqueio(motivo);
        usuario.setBloqueadoEm(LocalDateTime.now());
        return toResponse(usuario);
    }

    @Transactional
    public UsuarioAdminResponseDTO desbloquear(Long id) {
        Usuario usuario = buscar(id);
        usuario.setSituacao(SituacaoUsuario.ATIVO);
        usuario.setMotivoBloqueio(null);
        usuario.setBloqueadoEm(null);
        return toResponse(usuario);
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .filter(u -> u.getSituacao() != SituacaoUsuario.EXCLUIDO)
                .orElseThrow(() -> new RecursoNaoEncontradoException("AUT-012", "Usuário não encontrado: " + id));
    }

    private UsuarioAdminResponseDTO toResponse(Usuario u) {
        String nome = investidorRepository.findById(u.getId()).map(Investidor::getNome).orElse(null);
        return new UsuarioAdminResponseDTO(u.getId(), u.getEmail(), nome, u.getPapel(), u.getSituacao(),
                u.getMotivoBloqueio(),
                carteiraRepository.countByInvestidorIdAndAtivaTrue(u.getId()),
                carteiraAcaoRepository.existsByCarteiraInvestidorIdAndCarteiraAtivaTrueAndQuantidadeGreaterThan(u.getId(), 0));
    }
}
