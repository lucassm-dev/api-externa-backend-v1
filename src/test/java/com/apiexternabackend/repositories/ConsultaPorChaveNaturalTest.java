package com.apiexternabackend.repositories;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guarda de arquitetura: consulta por chave natural (ticker, CNPJ, e-mail, CPF) tem que
 * filtrar por ativo. Sem o filtro, uma chave reciclada (linha inativa + linha ativa com o
 * mesmo valor) faz a consulta que devolve Optional estourar
 * IncorrectResultSizeDataAccessException — foi exatamente o 500 de
 * GET /corretoras/cnpj/{cnpj} corrigido em 06/09/2026.
 */
class ConsultaPorChaveNaturalTest {

    private static final List<Class<?>> REPOSITORIOS = List.of(
            AcaoRepository.class, CorretoraRepository.class, InvestidorRepository.class,
            CarteiraRepository.class, CarteiraAcaoRepository.class, OperacaoRepository.class);

    private static final List<String> CHAVES_NATURAIS = List.of("Ticker", "Cnpj", "Email", "Cpf");

    @Test
    @DisplayName("@spec:AC-509 Nenhum repositório consulta por chave natural sem filtrar ativo")
    void nenhumaConsultaPorChaveNaturalSemFiltroDeAtivo() {
        List<String> semFiltro = REPOSITORIOS.stream()
                .flatMap(repo -> Stream.of(repo.getDeclaredMethods())
                        .map(Method::getName)
                        .filter(this::consultaPorChaveNatural)
                        .filter(nome -> !nome.contains("AtivoTrue") && !nome.contains("AtivaTrue"))
                        .map(nome -> repo.getSimpleName() + "." + nome))
                .toList();

        assertThat(semFiltro)
                .as("consultas por chave natural sem filtro de ativo (risco de 500 com chave reciclada)")
                .isEmpty();
    }

    private boolean consultaPorChaveNatural(String nomeDoMetodo) {
        if (!nomeDoMetodo.startsWith("findBy") && !nomeDoMetodo.startsWith("existsBy")) {
            return false;
        }
        return CHAVES_NATURAIS.stream().anyMatch(chave -> nomeDoMetodo.contains("By" + chave));
    }
}
