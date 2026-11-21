package com.apiexternabackend.config;

import com.apiexternabackend.domains.Usuario;
import com.apiexternabackend.domains.enums.Papel;
import com.apiexternabackend.domains.enums.SituacaoUsuario;
import com.apiexternabackend.repositories.UsuarioRepository;
import com.apiexternabackend.services.AutenticacaoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * spec-16 (US-454): cria o primeiro ADMIN a partir de ADMIN_EMAIL / ADMIN_SENHA. A senha nunca vai para o log.
 */
@Slf4j
@Component
public class AdminInicial implements ApplicationRunner {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String senha;

    public AdminInicial(UsuarioRepository repository, PasswordEncoder passwordEncoder,
                        @Value("${admin.email:}") String email, @Value("${admin.senha:}") String senha) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.email = email.trim();
        this.senha = senha;
    }

    @Override
    public void run(ApplicationArguments args) {
        // ASM-474: com um ADMIN existente as variáveis são ignoradas
        if (repository.existsByPapelAndSituacaoNot(Papel.ADMIN, SituacaoUsuario.EXCLUIDO)) {
            return;
        }
        if (email.isEmpty() || senha.isEmpty()) {
            log.warn("ADMIN_EMAIL ou ADMIN_SENHA não definidos: nenhum ADMIN foi criado.");
            return;
        }
        if (repository.existsByEmailAndSituacaoNot(email, SituacaoUsuario.EXCLUIDO)) {
            log.warn("ADMIN_EMAIL {} já pertence a um usuário: nenhum ADMIN foi criado. Troque o e-mail.", email);
            return;
        }
        if (!AutenticacaoService.POLITICA_SENHA.matcher(senha).matches()) {
            log.warn("ADMIN_SENHA fora da política (mínimo 8 caracteres, com letra e número): nenhum ADMIN foi criado.");
            return;
        }
        repository.save(new Usuario(null, email, passwordEncoder.encode(senha), Papel.ADMIN, SituacaoUsuario.ATIVO));
        log.info("ADMIN inicial criado: {}", email);
    }
}
