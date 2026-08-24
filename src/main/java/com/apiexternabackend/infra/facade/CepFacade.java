package com.apiexternabackend.infra.facade;

import com.apiexternabackend.infra.client.cep.CepClient;
import com.apiexternabackend.infra.client.cep.dto.CepResponseDTO;
import com.apiexternabackend.resources.exceptions.BusinessException;
import com.apiexternabackend.resources.exceptions.ExternalServiceException;
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
                throw new BusinessException("CEP não encontrado: " + cep);
            }
            return resp;
        } catch (BusinessException e) {
            throw e;
        } catch (FeignException e) {
            throw new ExternalServiceException("Serviço de CEP indisponível. Tente novamente mais tarde.", e);
        }
    }
}