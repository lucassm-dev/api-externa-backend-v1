package com.apiexternabackend.services;

import com.apiexternabackend.domains.Corretora;
import com.apiexternabackend.domains.dtos.CorretoraRequestDTO;
import com.apiexternabackend.domains.dtos.CorretoraResponseDTO;
import com.apiexternabackend.infra.client.cnpj.dto.CnpjResponseDTO;
import com.apiexternabackend.infra.client.cep.dto.CepResponseDTO;
import com.apiexternabackend.infra.facade.CnpjFacade;
import com.apiexternabackend.infra.facade.CepFacade;
import com.apiexternabackend.infra.facade.CvmFacade;
import com.apiexternabackend.infra.facade.CvmFacade.ResultadoVerificacaoCvm;
import com.apiexternabackend.mappers.CorretoraMapper;
import com.apiexternabackend.repositories.CorretoraRepository;
import com.apiexternabackend.resources.exceptions.BusinessException;
import com.apiexternabackend.resources.exceptions.DuplicateResourceException;
import com.apiexternabackend.resources.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CorretoraService {

    private final CorretoraRepository repository;
    private final CorretoraMapper mapper;
    private final CnpjFacade cnpjFacade;
    private final CepFacade cepFacade;
    private final CvmFacade cvmFacade;

    public CorretoraResponseDTO cadastrar(CorretoraRequestDTO dto) {
        String cnpj = cnpjFacade.normalizar(dto.getCnpj());

        cnpjFacade.validar(cnpj);

        if (repository.existsByCnpj(cnpj)) {
            throw new DuplicateResourceException("Corretora já cadastrada com o CNPJ: " + cnpj);
        }

        // Busca dados cadastrais (Receita/BrasilAPI) — AC-103
        CnpjResponseDTO dadosCnpj = cnpjFacade.buscar(cnpj);

        // Verifica autorização na CVM — AC-105, AC-106, AC-107
        ResultadoVerificacaoCvm resultadoCvm = cvmFacade.verificar(cnpj);

        if (resultadoCvm.falhaVerificacao()) {
            throw new BusinessException(resultadoCvm.mensagem());
        }
        if (!resultadoCvm.autorizada()) {
            throw new BusinessException(resultadoCvm.mensagem());
        }

        // Busca endereço pelo CEP — AC-102
        String cep = extrairCep(dadosCnpj);
        CepResponseDTO dadosCep = null;
        if (cep != null && !cep.isBlank()) {
            dadosCep = cepFacade.buscar(cep);
        }

        Corretora corretora = construir(cnpj, dadosCnpj, dadosCep, resultadoCvm);
        return mapper.toResponse(repository.save(corretora));
    }

    public Page<CorretoraResponseDTO> listar(Pageable pageable) {
        return repository.findAllByAtivoTrue(pageable).map(mapper::toResponse);
    }

    public void excluir(Long id) {
        Corretora corretora = repository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Corretora não encontrada: " + id));
        corretora.setAtivo(false);
        repository.save(corretora);
    }

    public CorretoraResponseDTO buscarPorId(Long id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Corretora não encontrada: " + id));
    }

    public CorretoraResponseDTO buscarPorCnpj(String cnpj) {
        return repository.findByCnpj(cnpjFacade.normalizar(cnpj))
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Corretora não encontrada: " + cnpj));
    }

    private String extrairCep(CnpjResponseDTO dados) {
        if (dados.getEndereco() != null) return dados.getEndereco().getCep();
        return null;
    }

    private Corretora construir(String cnpj, CnpjResponseDTO cnpjData, CepResponseDTO cepData,
                                ResultadoVerificacaoCvm cvm) {
        Corretora c = new Corretora();
        c.setCnpj(cnpj);
        c.setRazaoSocial(cnpjData.getRazaoSocial());
        c.setNomeFantasia(cnpjData.getNomeFantasia());
        c.setEmail(cnpjData.getEmail());
        c.setTelefone(cnpjData.getTelefone());
        c.setSituacaoCadastral(cnpjData.getSituacaoCadastral());
        c.setValidadaNaCvm(true);
        c.setDataBaseCvm(cvm.dataBase());

        if (cepData != null) {
            c.setCep(cepData.getCep());
            c.setLogradouro(cepData.getLogradouro());
            c.setBairro(cepData.getBairro());
            c.setCidade(cepData.getLocalidade());
            c.setUf(cepData.getUf());
        } else if (cnpjData.getEndereco() != null) {
            CnpjResponseDTO.CnpjEnderecoDTO end = cnpjData.getEndereco();
            c.setCep(end.getCep());
            c.setLogradouro(end.getLogradouro());
            c.setNumero(end.getNumero());
            c.setComplemento(end.getComplemento());
            c.setBairro(end.getBairro());
            c.setCidade(end.getMunicipio());
            c.setUf(end.getUf());
        }

        return c;
    }
}
