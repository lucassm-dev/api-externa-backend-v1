package com.apiexternabackend.infra.facade;

import com.apiexternabackend.infra.client.cnpj.CnpjClient;
import com.apiexternabackend.resources.exceptions.RegraVioladaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

@ExtendWith(MockitoExtension.class)
class CnpjFacadeTest {

    private static final String CNPJ_VALIDO = "02332886000104";
    private static final String CNPJ_FORMATADO = "02.332.886/0001-04";
    private static final String CNPJ_DIGITOS_INVALIDOS = "12345678000100";

    @Mock
    private CnpjClient client;

    @InjectMocks
    private CnpjFacade facade;

    @Test
    @DisplayName("@spec:AC-415 CNPJ formatado é normalizado para 14 dígitos")
    void deveNormalizarCnpjFormatado() {
        String resultado = facade.normalizar(CNPJ_FORMATADO);

        assertThat(resultado).isEqualTo(CNPJ_VALIDO);
        assertThat(resultado).hasSize(14);
        assertThat(resultado).matches("\\d{14}");
    }

    @Test
    @DisplayName("@spec:AC-416 CNPJ com dígitos verificadores válidos passa a validação")
    void deveAceitarCnpjValido() {
        assertThatNoException().isThrownBy(() -> facade.validar(CNPJ_VALIDO));
    }

    @Test
    @DisplayName("@spec:AC-417 CNPJ com dígitos verificadores inválidos é rejeitado")
    void deveRejeitarCnpjComDigitosInvalidos() {
        assertThatThrownBy(() -> facade.validar(CNPJ_DIGITOS_INVALIDOS))
                .isInstanceOf(RegraVioladaException.class)
                .hasMessageContaining("dígitos verificadores inválidos");
    }

    @Test
    @DisplayName("@spec:AC-418 CNPJ com comprimento diferente de 14 é rejeitado")
    void deveRejeitarCnpjComComprimentoErrado() {
        assertThatThrownBy(() -> facade.validar("1234"))
                .isInstanceOf(RegraVioladaException.class)
                .hasMessageContaining("inválido");
    }

    @Test
    @DisplayName("@spec:AC-418 CNPJ nulo é rejeitado como formato inválido")
    void deveRejeitarCnpjNulo() {
        assertThatThrownBy(() -> facade.validar(null))
                .isInstanceOf(RegraVioladaException.class);
    }

    @Test
    @DisplayName("@spec:AC-433 CNPJ inválido traz o código COR-003 do catálogo")
    void deveTrazerCodigoCor003AoRejeitarCnpjInvalido() {
        assertThatThrownBy(() -> facade.validar(CNPJ_DIGITOS_INVALIDOS))
                .isInstanceOf(RegraVioladaException.class)
                .extracting(e -> ((RegraVioladaException) e).getCodigo())
                .isEqualTo("COR-003");
    }
}
