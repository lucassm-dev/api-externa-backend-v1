package com.apiexternabackend.services;

import com.apiexternabackend.domains.CodigoRecuperacao;
import com.apiexternabackend.domains.Usuario;
import com.apiexternabackend.domains.enums.SituacaoUsuario;
import com.apiexternabackend.repositories.CodigoRecuperacaoRepository;
import com.apiexternabackend.repositories.UsuarioRepository;
import com.apiexternabackend.resources.exceptions.CredenciaisInvalidasException;
import com.apiexternabackend.resources.exceptions.IntegracaoExternaException;
import com.apiexternabackend.resources.exceptions.RecursoDuplicadoException;
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
import java.time.Instant;

import static com.apiexternabackend.services.RecuperacaoSenhaService.JANELA_PEDIDOS;
import static com.apiexternabackend.services.RecuperacaoSenhaService.MAX_PEDIDOS;
import static com.apiexternabackend.services.RecuperacaoSenhaService.MAX_TENTATIVAS;
import static com.apiexternabackend.services.RecuperacaoSenhaService.VALIDADE;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrocaEmailService {

    private final SecureRandom random = new SecureRandom();
    private final UsuarioRepository usuarioRepository;
    private final CodigoRecuperacaoRepository codigoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${mail.from:}")
    private String remetente;

    @Transactional
    public void pedir(Long usuarioId, String novoEmail, String senhaAtual) {
        Usuario usuario = usuario(usuarioId);
        if (!passwordEncoder.matches(senhaAtual, usuario.getSenha())) {
            throw new RegraVioladaException("AUT-014", "Senha atual incorreta.");
        }
        exigirEmailLivre(novoEmail);
        Instant agora = Instant.now();
        if (codigoRepository.countByUsuarioAndNovoEmailIsNotNullAndCriadoEmAfter(usuario, agora.minus(JANELA_PEDIDOS)) >= MAX_PEDIDOS) {
            throw new RegraVioladaException("AUT-015", "Muitos pedidos de código. Tente de novo em alguns minutos.");
        }

        String codigo = String.format("%06d", random.nextInt(1_000_000));
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(remetente);
            msg.setTo(novoEmail);
            msg.setSubject("Código para confirmar seu novo e-mail");
            msg.setText("Seu código é " + codigo + ". Ele vale por " + VALIDADE.toMinutes()
                    + " minutos. Se não foi você, ignore este e-mail.");
            mailSender.send(msg);
        } catch (MailException e) {
            // ADR-016: em fluxo autenticado a falha aparece, e nada pendente é gravado
            log.warn("Falha ao enviar código de troca de e-mail do usuário {}: {}", usuario.getId(), e.getMessage());
            throw new IntegracaoExternaException("EXT-012", "Serviço de e-mail indisponível, tente mais tarde.", false, e);
        }

        codigoRepository.findByUsuarioAndNovoEmailIsNotNullAndUsadoEmIsNull(usuario).forEach(c -> c.setUsadoEm(agora));
        CodigoRecuperacao novo = new CodigoRecuperacao();
        novo.setUsuario(usuario);
        novo.setNovoEmail(novoEmail);
        novo.setCodigoHash(passwordEncoder.encode(codigo));
        novo.setCriadoEm(agora);
        novo.setExpiraEm(agora.plus(VALIDADE));
        codigoRepository.save(novo);
    }

    // noRollbackFor: a tentativa errada precisa ficar gravada mesmo com a exceção
    @Transactional(noRollbackFor = RegraVioladaException.class)
    public void confirmar(Long usuarioId, String codigo) {
        Usuario usuario = usuario(usuarioId);
        CodigoRecuperacao atual = codigoRepository.findFirstByUsuarioAndNovoEmailIsNotNullAndUsadoEmIsNullOrderByCriadoEmDesc(usuario)
                .filter(c -> c.getExpiraEm().isAfter(Instant.now()) && c.getTentativas() < MAX_TENTATIVAS)
                .orElseThrow(this::codigoInvalido);
        if (!passwordEncoder.matches(codigo, atual.getCodigoHash())) {
            atual.setTentativas(atual.getTentativas() + 1);
            throw codigoInvalido();
        }
        // alguém pode ter ocupado o endereço entre o pedido e a confirmação
        exigirEmailLivre(atual.getNovoEmail());

        atual.setUsadoEm(Instant.now());
        usuario.setEmail(atual.getNovoEmail());
    }

    private Usuario usuario(Long id) {
        return usuarioRepository.findById(id)
                .filter(u -> u.getSituacao() != SituacaoUsuario.EXCLUIDO)
                .orElseThrow(() -> new CredenciaisInvalidasException("AUT-005", "Token ausente ou inválido."));
    }

    private void exigirEmailLivre(String email) {
        if (usuarioRepository.existsByEmailAndSituacaoNot(email, SituacaoUsuario.EXCLUIDO)) {
            throw new RecursoDuplicadoException("AUT-001", "E-mail já está em uso: " + email);
        }
    }

    private RegraVioladaException codigoInvalido() {
        return new RegraVioladaException("AUT-013", "Código inválido ou expirado.");
    }
}
