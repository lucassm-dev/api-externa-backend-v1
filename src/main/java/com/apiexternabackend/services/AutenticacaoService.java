package com.apiexternabackend.services;

import com.apiexternabackend.config.JwtService;
import com.apiexternabackend.domains.Investidor;
import com.apiexternabackend.domains.Usuario;
import com.apiexternabackend.domains.dtos.AuthCadastroRequestDTO;
import com.apiexternabackend.domains.dtos.AuthLoginRequestDTO;
import com.apiexternabackend.domains.dtos.AuthTokenResponseDTO;
import com.apiexternabackend.domains.dtos.InvestidorResponseDTO;
import com.apiexternabackend.domains.dtos.TrocarSenhaRequestDTO;
import com.apiexternabackend.domains.dtos.UsuarioLogadoResponseDTO;
import com.apiexternabackend.domains.enums.Papel;
import com.apiexternabackend.domains.enums.SituacaoUsuario;
import com.apiexternabackend.mappers.InvestidorMapper;
import com.apiexternabackend.repositories.FotoPerfilRepository;
import com.apiexternabackend.repositories.InvestidorRepository;
import com.apiexternabackend.repositories.UsuarioRepository;
import com.apiexternabackend.resources.exceptions.AcessoNegadoException;
import com.apiexternabackend.resources.exceptions.CredenciaisInvalidasException;
import com.apiexternabackend.resources.exceptions.RecursoDuplicadoException;
import com.apiexternabackend.resources.exceptions.RegraVioladaException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AutenticacaoService {

    public static final Pattern POLITICA_SENHA = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");
    private static final String MENSAGEM_LOGIN_INVALIDO = "E-mail ou senha inválidos.";

    private final InvestidorRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final InvestidorMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final FotoPerfilRepository fotoPerfilRepository;

    @Transactional
    public InvestidorResponseDTO cadastrar(AuthCadastroRequestDTO dto) {
        // AC-505/AC-506/AC-507: duplicidade só entre contas não excluídas — e-mail e CPF de
        // conta excluída podem ser reaproveitados, igual a ticker e CNPJ desde a SPEC-03
        if (usuarioRepository.existsByEmailAndSituacaoNot(dto.getEmail(), SituacaoUsuario.EXCLUIDO)) {
            throw new RecursoDuplicadoException("AUT-001", "E-mail já está em uso: " + dto.getEmail());
        }
        if (repository.existsByCpfAndAtivoTrue(dto.getCpf())) {
            throw new RecursoDuplicadoException("AUT-002", "CPF já está em uso: " + dto.getCpf());
        }
        if (!POLITICA_SENHA.matcher(dto.getSenha()).matches()) {
            throw new RegraVioladaException("AUT-008",
                    "Senha deve ter no mínimo 8 caracteres, com pelo menos uma letra e um número.");
        }

        // AC-553/AC-554: usuario e investidor na mesma transação
        Usuario usuario = usuarioRepository.save(new Usuario(null, dto.getEmail(),
                passwordEncoder.encode(dto.getSenha()), Papel.INVESTIDOR, SituacaoUsuario.ATIVO));

        Investidor investidor = new Investidor();
        investidor.setUsuario(usuario);
        investidor.setNome(dto.getNome());
        investidor.setCpf(dto.getCpf());
        investidor.setCriadoEm(LocalDateTime.now());

        return mapper.toResponse(repository.save(investidor));
    }

    public AuthTokenResponseDTO login(AuthLoginRequestDTO dto) {
        // AC-503/AC-508: conta excluída não entra. Ignorar excluídos também é o que permite
        // reaproveitar e-mail (AC-505) sem a consulta encontrar duas linhas.
        Usuario usuario = usuarioRepository.findByEmailAndSituacaoNot(dto.getEmail(), SituacaoUsuario.EXCLUIDO)
                .filter(u -> passwordEncoder.matches(dto.getSenha(), u.getSenha()))
                .orElseThrow(() -> new CredenciaisInvalidasException("AUT-004", MENSAGEM_LOGIN_INVALIDO));

        // AC-552: o bloqueio só é revelado a quem acertou a senha
        if (usuario.getSituacao() == SituacaoUsuario.BLOQUEADO) {
            throw new AcessoNegadoException("AUT-009", "Conta bloqueada. Procure o suporte.");
        }

        Jwt token = jwtService.gerar(usuario.getId(), usuario.getPapel());
        return new AuthTokenResponseDTO(token.getTokenValue(), "Bearer", token.getExpiresAt());
    }

    // #27: o token devolvido mantém logado só quem trocou; os anteriores caem pelo senhaAlteradaEm
    @Transactional
    public AuthTokenResponseDTO trocarSenha(Long usuarioId, TrocarSenhaRequestDTO dto) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new CredenciaisInvalidasException("AUT-005", "Token ausente ou inválido."));
        if (!passwordEncoder.matches(dto.getSenhaAtual(), usuario.getSenha())) {
            throw new RegraVioladaException("AUT-014", "Senha atual incorreta.");
        }
        if (!POLITICA_SENHA.matcher(dto.getNovaSenha()).matches()) {
            throw new RegraVioladaException("AUT-008",
                    "Senha deve ter no mínimo 8 caracteres, com pelo menos uma letra e um número.");
        }
        usuario.setSenha(passwordEncoder.encode(dto.getNovaSenha()));
        // iat do JWT tem precisão de segundos: o token novo, emitido no mesmo segundo, continua válido
        usuario.setSenhaAlteradaEm(Instant.now().truncatedTo(ChronoUnit.SECONDS));

        Jwt token = jwtService.gerar(usuario.getId(), usuario.getPapel());
        return new AuthTokenResponseDTO(token.getTokenValue(), "Bearer", token.getExpiresAt());
    }

    public UsuarioLogadoResponseDTO usuarioLogado(Long usuarioId) {
        // AC-573: conta excluída com token ainda válido responde como token inválido
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .filter(u -> u.getSituacao() != SituacaoUsuario.EXCLUIDO)
                .orElseThrow(() -> new CredenciaisInvalidasException("AUT-005", "Token ausente ou inválido."));
        String nome = repository.findById(usuarioId).map(Investidor::getNome).orElse(null);
        String fotoUrl = fotoPerfilRepository.existsById(usuarioId) ? "/auth/me/foto" : null;
        return new UsuarioLogadoResponseDTO(usuario.getId(), nome, usuario.getEmail(), usuario.getPapel(), fotoUrl);
    }
}
