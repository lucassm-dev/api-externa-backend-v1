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
import com.apiexternabackend.repositories.CarteiraAcaoRepository;
import com.apiexternabackend.resources.exceptions.IntegracaoExternaException;
import com.apiexternabackend.resources.exceptions.RecursoDuplicadoException;
import com.apiexternabackend.resources.exceptions.RecursoNaoEncontradoException;
import com.apiexternabackend.resources.exceptions.RegraVioladaException;
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
    private final CarteiraAcaoRepository carteiraAcaoRepository;
    private final AcaoMapper mapper;
    private final BrapiAdapter brapiAdapter;
    private final TwelveDataAdapter twelveDataAdapter;

    public AcaoResponseDTO cadastrar(AcaoRequestDTO dto) {
        String ticker = dto.getTicker().toUpperCase().trim();

        if (repository.existsByTickerAndAtivoTrue(ticker)) {
            throw new RecursoDuplicadoException("ACA-002", "Ação já cadastrada com o ticker: " + ticker);
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
        return repository.findByTickerAndAtivoTrue(ticker.toUpperCase().trim())
                .map(mapper::toResponse)
                .orElseThrow(() -> new RecursoNaoEncontradoException("ACA-001", "Ação não encontrada: " + ticker));
    }

    public void excluir(String ticker) {
        Acao acao = repository.findByTickerAndAtivoTrue(ticker.toUpperCase().trim())
                .orElseThrow(() -> new RecursoNaoEncontradoException("ACA-001", "Ação não encontrada: " + ticker));

        long vinculos = carteiraAcaoRepository.countByAcaoIdAndQuantidadeGreaterThan(acao.getId(), 0);
        if (vinculos > 0) {
            throw new RegraVioladaException("ACA-003",
                    "Ação possui posição ativa em " + vinculos + " carteira(s) — exclusão bloqueada");
        }

        acao.setAtivo(false);
        repository.save(acao);
    }

    public AcaoResponseDTO atualizarCotacao(Long id) {
        Acao acao = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("ACA-001", "Ação não encontrada: " + id));

        try {
            CotacaoAdapter adapter = adapterPara(acao.getMercado());
            CotacaoResultado cotacao = adapter.buscarCotacao(acao.getTicker());
            acao.setCotacaoAtual(cotacao.preco());
            acao.setDataHoraCotacao(cotacao.dataHora());
            return mapper.toResponse(repository.save(acao));
        } catch (IntegracaoExternaException e) {
            if (e.isLimiteExcedido()) {
                // AC-431: cota estourada não é silenciada — propaga 429 explícito
                throw e;
            }
            // RN-Q05/AC-210/AC-432: fonte indisponível (não é cota) → retorna última cotação conhecida
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
