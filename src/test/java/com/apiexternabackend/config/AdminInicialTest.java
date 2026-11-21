package com.apiexternabackend.config;

import com.apiexternabackend.domains.Usuario;
import com.apiexternabackend.domains.enums.Papel;
import com.apiexternabackend.domains.enums.SituacaoUsuario;
import com.apiexternabackend.repositories.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec-16 (US-454): o runner é chamado direto, como a inicialização faria, contra um banco isolado.
 */
@DataJpaTest
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class AdminInicialTest {

    private static final String EMAIL = "admin@empresa.com";
    private static final String SENHA = "Segredo123xyz";

    @Autowired private UsuarioRepository repository;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("@spec:AC-564 Duas inicializações criam exatamente um ADMIN ATIVO com hash que confere com a senha")
    void criaAdminUmaUnicaVez() {
        AdminInicial runner = new AdminInicial(repository, encoder, EMAIL, SENHA);
        runner.run(null);
        runner.run(null);

        List<Usuario> admins = admins();
        assertThat(admins).hasSize(1);
        Usuario admin = admins.get(0);
        assertThat(admin.getEmail()).isEqualTo(EMAIL);
        assertThat(admin.getSituacao()).isEqualTo(SituacaoUsuario.ATIVO);
        assertThat(admin.getSenha()).isNotEqualTo(SENHA);
        assertThat(encoder.matches(SENHA, admin.getSenha())).isTrue();
    }

    @Test
    @DisplayName("@spec:AC-565 A senha do ADMIN não aparece no log e src/main não tem valor padrão para ela")
    void senhaNaoApareceEmLogNemNoCodigo(CapturedOutput saida) throws IOException {
        new AdminInicial(repository, encoder, EMAIL, SENHA).run(null);

        assertThat(saida.getAll()).contains("ADMIN inicial criado").doesNotContain(SENHA);

        // valor padrão = algo não vazio depois de "admin.senha:" / "ADMIN_SENHA=" etc.; "${admin.senha:}" é vazio
        Pattern comPadrao = Pattern.compile("(?i)(admin\\.senha|ADMIN_SENHA)\\s*[:=]\\s*[^}\\s]");
        try (Stream<Path> arquivos = Files.walk(Path.of("src/main"))) {
            List<Path> comValor = arquivos.filter(Files::isRegularFile)
                    .filter(p -> conteudo(p).lines().anyMatch(l -> comPadrao.matcher(l).find()))
                    .toList();
            assertThat(comValor).isEmpty();
        }
    }

    @Test
    @DisplayName("@spec:AC-566 Sem ADMIN_EMAIL ou ADMIN_SENHA sobe sem ADMIN e avisa no log")
    void semVariaveisNaoCriaEAvisa(CapturedOutput saida) {
        new AdminInicial(repository, encoder, "", SENHA).run(null);
        new AdminInicial(repository, encoder, EMAIL, "").run(null);

        assertThat(admins()).isEmpty();
        assertThat(saida.getAll().split("nenhum ADMIN foi criado", -1)).hasSize(3);
    }

    @Test
    @DisplayName("@spec:AC-567 E-mail de investidor não excluído não vira ADMIN e o log pede para trocar o e-mail")
    void emailDeInvestidorNaoViraAdmin(CapturedOutput saida) {
        Usuario investidor = repository.save(new Usuario(null, EMAIL, "hash", Papel.INVESTIDOR, SituacaoUsuario.ATIVO));

        new AdminInicial(repository, encoder, EMAIL, SENHA).run(null);

        assertThat(admins()).isEmpty();
        assertThat(repository.findById(investidor.getId()).orElseThrow().getPapel()).isEqualTo(Papel.INVESTIDOR);
        assertThat(saida.getAll()).contains("Troque o e-mail");
    }

    private List<Usuario> admins() {
        return repository.findAll().stream().filter(u -> u.getPapel() == Papel.ADMIN).toList();
    }

    private static String conteudo(Path p) {
        try {
            return Files.readString(p);
        } catch (IOException e) {
            return "";
        }
    }
}
