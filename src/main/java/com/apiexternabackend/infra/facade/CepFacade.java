package com.apiexternabackend.infra.facade;

import com.apiexternabackend.infra.client.cep.CepClient;
import com.apiexternabackend.infra.client.cep.dto.CepResponseDTO;
import com.apiexternabackend.resources.exceptions.IntegracaoExternaException;
import com.apiexternabackend.resources.exceptions.RegraVioladaException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CepFacade {

    private final CepClient client;

    public CepResponseDTO buscar(String cep) {
        String cepLimpo = cep.replaceAll("[^0-9]", "");
        try {
            CepResponseDTO resp = client.buscarPorCep(cepLimpo);
            if (resp == null || !resp.isValido()) {
                throw new RegraVioladaException("COR-003", "CEP não encontrado: " + cep);
            }
            return resp;
        } catch (RegraVioladaException e) {
            throw e;
        } catch (FeignException e) {
            throw new IntegracaoExternaException("EXT-007",
                    "Serviço de CEP indisponível. Tente novamente mais tarde.", false, e);
        }
    }
}