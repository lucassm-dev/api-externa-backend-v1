package com.apiexternabackend.infra.facade;

import com.apiexternabackend.infra.client.cnpj.CnpjClient;
import com.apiexternabackend.infra.client.cnpj.dto.CnpjResponseDTO;
import com.apiexternabackend.resources.exceptions.BusinessException;
import com.apiexternabackend.resources.exceptions.ExternalServiceException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CnpjFacade {

    private final CnpjClient client;

    public CnpjResponseDTO buscar(String cnpj) {
        try {
            return client.buscarPorCnpj(cnpj);
        } catch (FeignException.NotFound e) {
            throw new BusinessException("CNPJ não encontrado na base da Receita: " + cnpj);
        } catch (FeignException e) {
            throw new ExternalServiceException("Serviço de CNPJ indisponível. Tente novamente mais tarde.", e);
        }
    }
}
