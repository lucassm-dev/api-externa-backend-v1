package com.apiexternabackend.deploy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class BackupScriptTest {

    private static final String SCRIPT = "backup.sh";

    private ScriptSandbox sandbox;
    private Path backupDir;

    @BeforeEach
    void setUp() throws IOException {
        sandbox = ScriptSandbox.criar(SCRIPT);
        sandbox.criarFrontendRepo();
        sandbox.criarEnv();
        backupDir = Files.createTempDirectory("backup-dir-");
    }

    private Map<String, String> envPadrao() {
        Map<String, String> env = new HashMap<>();
        env.put("BACKUP_DIR", backupDir.toAbsolutePath().toString());
        return env;
    }

    private List<Path> dumps() {
        try (Stream<Path> arquivos = Files.list(backupDir)) {
            return arquivos.filter(p -> p.getFileName().toString().endsWith(".sql.gz"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    @DisplayName("@spec:AC-534 Backup gera dump compactado e datado")
    void geraDumpCompactadoEDatado() throws IOException {
        ScriptSandbox.Resultado resultado = sandbox.executar(SCRIPT, envPadrao());

        assertThat(resultado.codigoSaida()).isZero();

        List<Path> dumps = dumps();
        assertThat(dumps).hasSize(1);

        Path dump = dumps.get(0);
        assertThat(dump.getFileName().toString()).matches("investimentos-\\d{4}-\\d{2}-\\d{2}_\\d{4}\\.sql\\.gz");

        String conteudo;
        try (GZIPInputStream in = new GZIPInputStream(Files.newInputStream(dump))) {
            conteudo = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertThat(conteudo).contains("fake pg_dump dump");
    }

    @Test
    @DisplayName("@spec:AC-535 Dumps com mais de 7 dias são removidos")
    void removeDumpsAntigos() throws IOException {
        Path antigo = backupDir.resolve("investimentos-2020-01-01_0000.sql.gz");
        Path recente = backupDir.resolve("investimentos-2020-01-08_0000.sql.gz");
        Files.writeString(antigo, "dump antigo");
        Files.writeString(recente, "dump recente");
        Files.setLastModifiedTime(antigo, FileTime.from(Instant.now().minus(8, ChronoUnit.DAYS)));
        Files.setLastModifiedTime(recente, FileTime.from(Instant.now().minus(1, ChronoUnit.DAYS)));

        ScriptSandbox.Resultado resultado = sandbox.executar(SCRIPT, envPadrao());

        assertThat(resultado.codigoSaida()).isZero();
        assertThat(Files.exists(antigo)).isFalse();
        assertThat(Files.exists(recente)).isTrue();
    }

    @Test
    @DisplayName("@spec:AC-536 Falha do dump não deixa arquivo enganoso")
    void falhaDoDumpNaoDeixaArquivo() {
        sandbox.definirFalhaDump();

        ScriptSandbox.Resultado resultado = sandbox.executar(SCRIPT, envPadrao());

        assertThat(resultado.codigoSaida()).isNotZero();
        assertThat(dumps()).isEmpty();
    }
}
