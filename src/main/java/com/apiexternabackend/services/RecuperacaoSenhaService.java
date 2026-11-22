package com.apiexternabackend.services;

import com.apiexternabackend.domains.CodigoRecuperacao;
import com.apiexternabackend.domains.Usuario;
import com.apiexternabackend.domains.enums.SituacaoUsuario;
import com.apiexternabackend.repositories.CodigoRecuperacaoRepository;
import com.apiexternabackend.repositories.UsuarioRepository;
import com.apiexternabackend.resources.exceptions.RegraVioladaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecuperacaoSenhaService {

    static final Duration VALIDADE = Duration.ofMinutes(10);
    static final int MAX_TENTATIVAS = 5;
    static final int MAX_PEDIDOS = 3;
    static final Duration JANELA_PEDIDOS = Duration.ofMinutes(15);
    private static final String MENSAGEM_CODIGO_INVALIDO = "Código inválido ou expirado.";

    private final SecureRandom random = new SecureRandom();
    private final UsuarioRepository usuarioRepository;
    private final CodigoRecuperacaoRepository codigoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${mail.from:}")
    private String remetente;

    // PRD 15.2: o chamador nunca sabe se o e-mail existe, se estourou o limite ou se o envio falhou
    @Transactional
    public void solicitar(String email) {
        Usuario usuario = usuarioRepository.findByEmailAndSituacaoNot(email, SituacaoUsuario.EXCLUIDO)
                .filter(u -> u.getSituacao() == SituacaoUsuario.ATIVO)
                .orElse(null);
        Instant agora = Instant.now();
        if (usuario == null
                || codigoRepository.countByUsuarioAndNovoEmailIsNullAndCriadoEmAfter(usuario, agora.minus(JANELA_PEDIDOS)) >= MAX_PEDIDOS) {
            return;
        }

        String codigo = String.format("%06d", random.nextInt(1_000_000));
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(remetente);
            msg.setTo(usuario.getEmail());
            msg.setSubject("Código para redefinir sua senha");
            msg.setText("Seu código é " + codigo + ". Ele vale por " + VALIDADE.toMinutes()
                    + " minutos. Se não foi você, ignore este e-mail.");
            mailSender.send(msg);
        } catch (MailException e) {
            // ADR-016: código não conta como enviado; log sem o código e sem o endereço completo
            log.warn("Falha ao enviar código de recuperação para o usuário {}: {}", usuario.getId(), e.getMessage());
            return;
        }

        codigoRepository.findByUsuarioAndNovoEmailIsNullAndUsadoEmIsNull(usuario).forEach(c -> c.setUsadoEm(agora));
        CodigoRecuperacao novo = new CodigoRecuperacao();
        novo.setUsuario(usuario);
        novo.setCodigoHash(passwordEncoder.encode(codigo));
        novo.setCriadoEm(agora);
        novo.setExpiraEm(agora.plus(VALIDADE));
        codigoRepository.save(novo);
    }

    // noRollbackFor: a tentativa errada precisa ficar gravada mesmo com a exceção
    @Transactional(noRollbackFor = RegraVioladaException.class)
    public void redefinir(String email, String codigo, String novaSenha) {
        Usuario usuario = usuarioRepository.findByEmailAndSituacaoNot(email, SituacaoUsuario.EXCLUIDO)
                .filter(u -> u.getSituacao() == SituacaoUsuario.ATIVO)
                .orElseThrow(this::codigoInvalido);
        CodigoRecuperacao atual = codigoRepository.findFirstByUsuarioAndNovoEmailIsNullAndUsadoEmIsNullOrderByCriadoEmDesc(usuario)
                .filter(c -> c.getExpiraEm().isAfter(Instant.now()) && c.getTentativas() < MAX_TENTATIVAS)
                .orElseThrow(this::codigoInvalido);

        if (!passwordEncoder.matches(codigo, atual.getCodigoHash())) {
            atual.setTentativas(atual.getTentativas() + 1);
            throw codigoInvalido();
        }
        if (!AutenticacaoService.POLITICA_SENHA.matcher(novaSenha).matches()) {
            throw new RegraVioladaException("AUT-008",
                    "Senha deve ter no mínimo 8 caracteres, com pelo menos uma letra e um número.");
        }

        Instant agora = Instant.now();
        atual.setUsadoEm(agora);
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        // iat do JWT tem precisão de segundos: truncar evita recusar o login feito no mesmo segundo
        usuario.setSenhaAlteradaEm(agora.truncatedTo(ChronoUnit.SECONDS));
    }

    private RegraVioladaException codigoInvalido() {
        return new RegraVioladaException("AUT-013", MENSAGEM_CODIGO_INVALIDO);
    }
}
