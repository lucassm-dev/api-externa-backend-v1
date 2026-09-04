package com.apiexternabackend.infra.facade;

import com.apiexternabackend.infra.client.cnpj.CnpjClient;
import com.apiexternabackend.infra.client.cnpj.dto.CnpjResponseDTO;
import com.apiexternabackend.resources.exceptions.IntegracaoExternaException;
import com.apiexternabackend.resources.exceptions.RegraVioladaException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CnpjFacade {

    private final CnpjClient client;

    public String normalizar(String cnpj) {
        return cnpj == null ? "" : cnpj.replaceAll("[^0-9]", "");
    }

    public void validar(String cnpj) {
        if (cnpj == null || cnpj.length() != 14 || !cnpj.matches("\\d{14}")) {
            throw new RegraVioladaException("COR-003", "CNPJ inválido: " + cnpj);
        }
        if (cnpj.chars().distinct().count() == 1 || !validarDigitos(cnpj)) {
            throw new RegraVioladaException("COR-003", "CNPJ com dígitos verificadores inválidos: " + cnpj);
        }
    }

    public CnpjResponseDTO buscar(String cnpj) {
        try {
            return client.buscarPorCnpj(cnpj);
        } catch (FeignException.NotFound e) {
            throw new RegraVioladaException("COR-003", "CNPJ não encontrado na base da Receita: " + cnpj);
        } catch (FeignException e) {
            throw new IntegracaoExternaException("EXT-007",
                    "Serviço de CNPJ indisponível. Tente novamente mais tarde.", false, e);
        }
    }

    private boolean validarDigitos(String cnpj) {
        int[] pesos1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] pesos2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        return calcDigito(cnpj, pesos1) == Character.getNumericValue(cnpj.charAt(12)) &&
               calcDigito(cnpj, pesos2) == Character.getNumericValue(cnpj.charAt(13));
    }

    private int calcDigito(String cnpj, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += Character.getNumericValue(cnpj.charAt(i)) * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
