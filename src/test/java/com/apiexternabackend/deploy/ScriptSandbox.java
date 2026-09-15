package com.apiexternabackend.deploy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Sandbox de processo para testar scripts de deploy sem tocar Docker, git ou
 * a rede de verdade. Cria um diretório temporário com um clone do script sob
 * teste, repositórios de backend/frontend falsos e binários falsos de
 * {@code git}, {@code docker} e {@code curl} na frente do PATH — cada
 * invocação fica registrada em {@code control/calls.log} para inspeção.
 * Genérico o bastante para ser reusado pelo script de backup (T-484).
 */
class ScriptSandbox {

    private static final Path REAL_DEPLOY_DIR = Paths.get("deploy");

    private final Path root;
    private final Path backendDir;
    private final Path binDir;
    private final Path controlDir;

    private ScriptSandbox(Path root, Path backendDir, Path binDir, Path controlDir) {
        this.root = root;
        this.backendDir = backendDir;
        this.binDir = binDir;
        this.controlDir = controlDir;
    }

    static ScriptSandbox criar(String scriptName) {
        try {
            Path root = Files.createTempDirectory("deploy-sandbox-");
            Path backendDir = root.resolve("backend");
            Path deployDir = backendDir.resolve("deploy");
            Files.createDirectories(deployDir);

            Path realScript = REAL_DEPLOY_DIR.resolve(scriptName);
            Path scriptCopy = deployDir.resolve(scriptName);
            Files.copy(realScript, scriptCopy, StandardCopyOption.REPLACE_EXISTING);
            tornarExecutavel(scriptCopy);

            Files.writeString(backendDir.resolve("docker-compose.yml"), "services: {}\n");
            Files.writeString(backendDir.resolve("docker-compose.prod.yml"), "services: {}\n");

            Path binDir = root.resolve("bin");
            Files.createDirectories(binDir);
            Path controlDir = root.resolve("control");
            Files.createDirectories(controlDir);
            Files.writeString(controlDir.resolve("calls.log"), "");

            ScriptSandbox sandbox = new ScriptSandbox(root, backendDir, binDir, controlDir);
            sandbox.instalarFakes();
            sandbox.initGitRepo(backendDir);
            return sandbox;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void instalarFakes() throws IOException {
        escreverFake("git", FAKE_GIT);
        escreverFake("docker", FAKE_DOCKER);
        escreverFake("curl", FAKE_CURL);
    }

    private void escreverFake(String nome, String conteudo) throws IOException {
        Path alvo = binDir.resolve(nome);
        Files.writeString(alvo, conteudo);
        tornarExecutavel(alvo);
    }

    private static void tornarExecutavel(Path arquivo) throws IOException {
        Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxr-xr-x");
        try {
            Files.setPosixFilePermissions(arquivo, perms);
        } catch (UnsupportedOperationException ignored) {
            arquivo.toFile().setExecutable(true);
        }
    }

    private void initGitRepo(Path dir) throws IOException {
        Files.createDirectories(dir);
    }

    Path backendDir() {
        return backendDir;
    }

    Path criarFrontendRepo() {
        try {
            Path frontendDir = root.resolve("frontend");
            initGitRepo(frontendDir);
            return frontendDir;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void criarEnv() {
        escrever(backendDir.resolve(".env"), "POSTGRES_DB=investimentos\nPOSTGRES_USER=investimentos\n");
    }

    void definirFalhaDump() {
        escrever(controlDir.resolve("dump-fail.txt"), "1\n");
    }

    void removerEnv() {
        try {
            Files.deleteIfExists(backendDir.resolve(".env"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void marcarSujo(Path repoDir) {
        escrever(repoDir.resolve(".dirty-marker"), "M arquivo-nao-commitado.txt\n");
    }

    void definirVersaoCompose(String versao) {
        escrever(controlDir.resolve("compose-version.txt"), versao + "\n");
    }

    void definirSaude(String container, String status) {
        escrever(controlDir.resolve("health-" + container + ".txt"), status + "\n");
    }

    void definirFalhaNoUp() {
        escrever(controlDir.resolve("up-fail.txt"), "1\n");
    }

    void definirCodigoSaude(String codigo) {
        escrever(controlDir.resolve("curl-saude-code.txt"), codigo + "\n");
    }

    void definirCodigoApi(String codigo) {
        escrever(controlDir.resolve("curl-api-code.txt"), codigo + "\n");
    }

    String frontendPathNoUp() {
        try {
            Path arquivo = controlDir.resolve("up-frontend-path.txt");
            return Files.exists(arquivo) ? Files.readString(arquivo, StandardCharsets.UTF_8) : null;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void definirCommitParaBranch(Path repoDir, String branch, String commit) {
        escrever(repoDir.resolve("COMMIT-" + branch), commit + "\n");
    }

    private void escrever(Path arquivo, String conteudo) {
        try {
            Files.writeString(arquivo, conteudo);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    List<String> chamadas() {
        try {
            Path log = controlDir.resolve("calls.log");
            if (!Files.exists(log)) {
                return List.of();
            }
            return Files.readAllLines(log, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    Resultado executar(String scriptName, Map<String, String> envExtra) {
        return executar(scriptName, envExtra, backendDir);
    }

    Resultado executar(String scriptName, Map<String, String> envExtra, Path diretorioDeTrabalho) {
        try {
            List<String> comando = new ArrayList<>();
            comando.add("bash");
            comando.add(backendDir.resolve("deploy").resolve(scriptName).toAbsolutePath().toString());

            ProcessBuilder pb = new ProcessBuilder(comando);
            pb.directory(diretorioDeTrabalho.toFile());
            pb.redirectErrorStream(false);

            Map<String, String> env = pb.environment();
            String pathOriginal = env.getOrDefault("PATH", "/usr/bin:/bin");
            env.put("PATH", binDir.toAbsolutePath() + java.io.File.pathSeparator + pathOriginal);
            env.putAll(envExtra);

            Process processo = pb.start();

            String stdout = new String(processo.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(processo.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean terminou = processo.waitFor(60, TimeUnit.SECONDS);
            if (!terminou) {
                processo.destroyForcibly();
                throw new IllegalStateException("script não terminou a tempo");
            }
            int codigo = processo.exitValue();
            return new Resultado(codigo, stdout, stderr);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    record Resultado(int codigoSaida, String stdout, String stderr) {
    }

    private static final String FAKE_GIT = """
            #!/usr/bin/env bash
            CONTROL_DIR="$(cd "$(dirname "$0")/.." && pwd)/control"
            echo "git $*" >> "$CONTROL_DIR/calls.log"

            dir="."
            subcmd=""
            rest=()
            i=1
            while [ $i -le $# ]; do
              arg="${!i}"
              if [ "$arg" = "-C" ]; then
                i=$((i+1))
                dir="${!i}"
              elif [ -z "$subcmd" ]; then
                subcmd="$arg"
              else
                rest+=("$arg")
              fi
              i=$((i+1))
            done

            case "$subcmd" in
              status)
                if [ -f "$dir/.dirty-marker" ]; then
                  cat "$dir/.dirty-marker"
                fi
                exit 0
                ;;
              fetch)
                exit 0
                ;;
              checkout)
                exit 0
                ;;
              reset)
                ultimo="${rest[${#rest[@]}-1]}"
                branch="${ultimo#origin/}"
                if [ -f "$dir/COMMIT-$branch" ]; then
                  cp "$dir/COMMIT-$branch" "$dir/COMMIT"
                else
                  echo "0000000" > "$dir/COMMIT"
                fi
                exit 0
                ;;
              rev-parse)
                if [ -f "$dir/COMMIT" ]; then
                  cat "$dir/COMMIT"
                else
                  echo "0000000"
                fi
                exit 0
                ;;
              *)
                exit 0
                ;;
            esac
            """;

    private static final String FAKE_DOCKER = """
            #!/usr/bin/env bash
            CONTROL_DIR="$(cd "$(dirname "$0")/.." && pwd)/control"
            echo "docker $*" >> "$CONTROL_DIR/calls.log"

            if [ "$1" = "compose" ]; then
              shift
              subcmd=""
              while [ $# -gt 0 ]; do
                case "$1" in
                  -f)
                    shift 2
                    ;;
                  *)
                    subcmd="$1"
                    break
                    ;;
                esac
              done
              case "$subcmd" in
                version)
                  if [ -f "$CONTROL_DIR/compose-version.txt" ]; then
                    cat "$CONTROL_DIR/compose-version.txt"
                  else
                    echo "Docker Compose version v2.24.0"
                  fi
                  exit 0
                  ;;
                up)
                  printf '%s' "${FRONTEND_PATH-<ausente>}" > "$CONTROL_DIR/up-frontend-path.txt"
                  if [ -f "$CONTROL_DIR/up-fail.txt" ]; then
                    echo "falha fake no up" >&2
                    exit 1
                  fi
                  exit 0
                  ;;
                exec)
                  if [ -f "$CONTROL_DIR/dump-fail.txt" ]; then
                    echo "erro fake no pg_dump" >&2
                    exit 1
                  fi
                  echo "-- fake pg_dump dump --"
                  exit 0
                  ;;
                *)
                  exit 0
                  ;;
              esac
            elif [ "$1" = "inspect" ]; then
              container="${!#}"
              arquivo="$CONTROL_DIR/health-$container.txt"
              if [ -f "$arquivo" ]; then
                cat "$arquivo"
              else
                echo "healthy"
              fi
              exit 0
            elif [ "$1" = "logs" ]; then
              container="${!#}"
              echo "log fake de $container"
              exit 0
            else
              exit 0
            fi
            """;

    private static final String FAKE_CURL = """
            #!/usr/bin/env bash
            CONTROL_DIR="$(cd "$(dirname "$0")/.." && pwd)/control"
            echo "curl $*" >> "$CONTROL_DIR/calls.log"

            url="${!#}"
            case "$url" in
              */saude)
                if [ -f "$CONTROL_DIR/curl-saude-code.txt" ]; then
                  cat "$CONTROL_DIR/curl-saude-code.txt"
                else
                  echo "200"
                fi
                ;;
              *)
                if [ -f "$CONTROL_DIR/curl-api-code.txt" ]; then
                  cat "$CONTROL_DIR/curl-api-code.txt"
                else
                  echo "401"
                fi
                ;;
            esac
            exit 0
            """;
}
