package com.apiexternabackend.services;

import com.apiexternabackend.domains.Acao;
import com.apiexternabackend.domains.dtos.AcaoRequestDTO;
import com.apiexternabackend.domains.dtos.AcaoResponseDTO;
import com.apiexternabackend.domains.enums.Mercado;
import com.apiexternabackend.infra.adapter.BrapiAdapter;
import com.apiexternabackend.infra.adapter.CotacaoAdapter;
import com.apiexternabackend.infra.adapter.CotacaoResultado;
import com.apiexternabackend.infra.adapter.TwelveDataAdapter;
import com.apiexternabackend.mappers.AcaoMapper;
import com.apiexternabackend.repositories.AcaoRepository;
import com.apiexternabackend.resources.exceptions.DuplicateResourceException;
import com.apiexternabackend.resources.exceptions.ExternalServiceException;
import com.apiexternabackend.resources.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcaoService {

    private final AcaoRepository repository;
    private final AcaoMapper mapper;
    private final BrapiAdapter brapiAdapter;
    private final TwelveDataAdapter twelveDataAdapter;

    public AcaoResponseDTO cadastrar(AcaoRequestDTO dto) {
        String ticker = dto.getTicker().toUpperCase().trim();

        if (repository.existsByTicker(ticker)) {
            throw new DuplicateResourceException("Ação já cadastrada com o ticker: " + ticker);
        }

        CotacaoAdapter adapter = adapterPara(dto.getMercado());
        CotacaoResultado cotacao = adapter.buscarCotacao(ticker);

        Acao acao = new Acao();
        acao.setTicker(ticker);
        acao.setMercado(dto.getMercado());
        acao.setMoeda(dto.getMercado() == Mercado.BR ? "BRL" : "USD");
        acao.setCotacaoAtual(cotacao.preco());
        acao.setDataHoraCotacao(cotacao.dataHora());

        return mapper.toResponse(repository.save(acao));
    }

    public Page<AcaoResponseDTO> listar(Pageable pageable) {
        return repository.findAllByAtivoTrue(pageable).map(mapper::toResponse);
    }

    public AcaoResponseDTO buscarPorTicker(String ticker) {
        return repository.findByTicker(ticker.toUpperCase().trim())
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Ação não encontrada: " + ticker));
    }

    public void excluir(String ticker) {
        Acao acao = repository.findByTickerAndAtivoTrue(ticker.toUpperCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Ação não encontrada: " + ticker));
        acao.setAtivo(false);
        repository.save(acao);
    }

    public AcaoResponseDTO atualizarCotacao(Long id) {
        Acao acao = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ação não encontrada: " + id));

        try {
            CotacaoAdapter adapter = adapterPara(acao.getMercado());
            CotacaoResultado cotacao = adapter.buscarCotacao(acao.getTicker());
            acao.setCotacaoAtual(cotacao.preco());
            acao.setDataHoraCotacao(cotacao.dataHora());
            return mapper.toResponse(repository.save(acao));
        } catch (ExternalServiceException e) {
            // RN-Q05: fonte indisponível → retorna última cotação conhecida
            log.warn("Fonte de cotação indisponível para {}: {}. Retornando última cotação conhecida.",
                    acao.getTicker(), e.getMessage());
            acao.setDataHoraCotacao(acao.getDataHoraCotacao() != null ? acao.getDataHoraCotacao() : LocalDateTime.now());
            return mapper.toResponse(acao);
        }
    }

    private CotacaoAdapter adapterPara(Mercado mercado) {
        return mercado == Mercado.BR ? brapiAdapter : twelveDataAdapter;
    }
}
