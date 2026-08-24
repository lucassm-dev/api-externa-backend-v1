package com.apiexternabackend.services;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Placeholder — autenticacao-investidor é 2ª fase (Spring Security + JWT).
 * Testes desabilitados até a feature ser implementada.
 */
@Disabled("autenticacao-investidor: 2ª fase — Spring Security + JWT não implementado no MVP")
class AutenticacaoServiceTest {

    @Test
    @DisplayName("@spec:AC-001 Cadastro com credenciais válidas cria a conta")
    void deveCadastrarComCredenciaisValidas() {
    }

    @Test
    @DisplayName("@spec:AC-002 Identificador de login é único")
    void deveRejeitarLoginDuplicado() {
    }

    @Test
    @DisplayName("@spec:AC-003 Senha nunca é guardada em texto puro")
    void deveSalvarSenhaComo() {
    }

    @Test
    @DisplayName("@spec:AC-004 Login com credenciais corretas autentica")
    void deveAutenticarComCredenciaisCorretas() {
    }

    @Test
    @DisplayName("@spec:AC-005 Login com credenciais erradas é recusado")
    void deveRecusarCredenciaisErradas() {
    }

    @Test
    @DisplayName("@spec:AC-006 Investidor só acessa os próprios dados")
    void deveIsolaçãoPorInvestidor() {
    }

    @Test
    @DisplayName("@spec:AC-007 Recurso protegido exige autenticação")
    void deveExigirAutenticacao() {
    }
}
