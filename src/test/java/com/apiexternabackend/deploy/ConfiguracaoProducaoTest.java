package com.apiexternabackend.deploy;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;

class ConfiguracaoProducaoTest {

    @SuppressWarnings("unchecked")
    private Map<String, Object> lerComposeMesclado() throws IOException {
        Yaml yaml = criarYamlComSuporteAReset();
        Map<String, Object> base;
        Map<String, Object> override;
        try (InputStream in = new FileInputStream("docker-compose.yml")) {
            base = yaml.load(in);
        }
        try (InputStream in = new FileInputStream("docker-compose.prod.yml")) {
            override = yaml.load(in);
        }
        Map<String, Object> servicosBase = (Map<String, Object>) base.get("services");
        Map<String, Object> servicosOverride = (Map<String, Object>) override.get("services");
        for (String nome : servicosOverride.keySet()) {
            Map<String, Object> servicoBase = (Map<String, Object>) servicosBase.get(nome);
            Map<String, Object> servicoOverride = (Map<String, Object>) servicosOverride.get(nome);
            servicoBase.putAll(servicoOverride);
        }
        return servicosBase;
    }

    private Yaml criarYamlComSuporteAReset() {
        return new Yaml(new ConstructorComSuporteAReset());
    }

    /**
     * O Docker Compose usa a tag {@code !reset} para zerar uma lista herdada
     * do compose base (ex.: {@code ports: !reset []}). O SnakeYAML não
     * conhece essa tag; tratamos como uma sequência comum.
     */
    private static final class ConstructorComSuporteAReset extends Constructor {
        ConstructorComSuporteAReset() {
            super(new LoaderOptions());
            this.yamlConstructors.put(new Tag("!reset"), new ConstructYamlSeqComoLista());
        }

        private class ConstructYamlSeqComoLista extends SafeConstructor.ConstructYamlSeq {
            @Override
            public Object construct(Node node) {
                node.setTag(Tag.SEQ);
                return super.construct(node);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void banco_sem_porta_publicada_em_producao() throws IOException {
        // @spec:AC-521
        Map<String, Object> servicos = lerComposeMesclado();
        Map<String, Object> postgres = (Map<String, Object>) servicos.get("postgres");
        List<Object> portas = (List<Object>) postgres.get("ports");
        assertThat(portas).isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Test
    void so_frontend_publica_porta_em_producao() throws IOException {
        // @spec:AC-522
        Map<String, Object> servicos = lerComposeMesclado();
        Map<String, Object> backend = (Map<String, Object>) servicos.get("backend");
        Map<String, Object> frontend = (Map<String, Object>) servicos.get("frontend");

        List<Object> portasBackend = (List<Object>) backend.get("ports");
        List<Object> portasFrontend = (List<Object>) frontend.get("ports");

        assertThat(portasBackend).isEmpty();
        assertThat(portasFrontend).isNotEmpty();
    }

    @SuppressWarnings("unchecked")
    @Test
    void api_em_perfil_de_producao_e_com_memoria_limitada() throws IOException {
        // @spec:AC-523
        Map<String, Object> servicos = lerComposeMesclado();
        Map<String, Object> backend = (Map<String, Object>) servicos.get("backend");
        Map<String, Object> environment = (Map<String, Object>) backend.get("environment");

        assertThat(environment.get("SPRING_PROFILES_ACTIVE")).isEqualTo("prod");
        assertThat(backend.get("mem_limit")).isNotNull();
    }

    @SuppressWarnings("unchecked")
    @Test
    void log_de_container_com_rotacao() throws IOException {
        // @spec:AC-524
        Map<String, Object> servicos = lerComposeMesclado();
        for (String nome : List.of("postgres", "backend", "frontend")) {
            Map<String, Object> servico = (Map<String, Object>) servicos.get(nome);
            Map<String, Object> logging = (Map<String, Object>) servico.get("logging");
            assertThat(logging).as("logging de " + nome).isNotNull();
            assertThat(logging.get("driver")).as("driver de " + nome).isEqualTo("json-file");

            Map<String, Object> options = (Map<String, Object>) logging.get("options");
            assertThat(options.get("max-size")).as("max-size de " + nome).isNotNull();
            assertThat(options.get("max-file")).as("max-file de " + nome).isNotNull();
        }
    }

    @Test
    void perfil_de_producao_nao_despeja_sql_no_log() throws IOException {
        // @spec:AC-525
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(
                Path.of("src/main/resources/application-prod.properties"))) {
            properties.load(in);
        }
        assertThat(properties.getProperty("spring.jpa.show-sql")).isEqualTo("false");
    }
}
