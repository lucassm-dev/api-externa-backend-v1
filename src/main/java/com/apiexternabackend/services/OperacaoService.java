package com.apiexternabackend.services;

import com.apiexternabackend.domains.Acao;
import com.apiexternabackend.domains.Carteira;
import com.apiexternabackend.domains.CarteiraAcao;
import com.apiexternabackend.domains.Operacao;
import com.apiexternabackend.domains.dtos.OperacaoRequestDTO;
import com.apiexternabackend.domains.dtos.OperacaoResponseDTO;
import com.apiexternabackend.domains.enums.TipoOperacao;
import com.apiexternabackend.infra.adapter.CotacaoResultado;
import com.apiexternabackend.infra.adapter.BrapiAdapter;
import com.apiexternabackend.infra.adapter.TwelveDataAdapter;
import com.apiexternabackend.infra.adapter.CotacaoAdapter;
import com.apiexternabackend.domains.enums.Mercado;
import com.apiexternabackend.mappers.OperacaoMapper;
import com.apiexternabackend.repositories.AcaoRepository;
import com.apiexternabackend.repositories.CarteiraAcaoRepository;
import com.apiexternabackend.repositories.OperacaoRepository;
import com.apiexternabackend.resources.exceptions.BusinessException;
import com.apiexternabackend.resources.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OperacaoService {

    private final OperacaoRepository operacaoRepository;
    private final AcaoRepository acaoRepository;
    private final CarteiraAcaoRepository carteiraAcaoRepository;
    private final CarteiraService carteiraService;
    private final PosicaoService posicaoService;
    private final OperacaoMapper mapper;
    private final BrapiAdapter brapiAdapter;
    private final TwelveDataAdapter twelveDataAdapter;

    @Transactional
    public OperacaoResponseDTO comprar(OperacaoRequestDTO dto) {
        Carteira carteira = carteiraService.buscarAtiva(dto.getCarteiraId()); // AC-309
        Acao acao = buscarAcao(dto.getTicker());

        validarMercado(carteira, acao); // AC-305/AC-403

        CotacaoResultado cotacao = adapterPara(acao.getMercado()).buscarCotacao(acao.getTicker()); // AC-401

        Operacao operacao = new Operacao();
        operacao.setCarteira(carteira);
        operacao.setAcao(acao);
        operacao.setTipo(TipoOperacao.COMPRA);
        operacao.setQuantidade(dto.getQuantidade());
        operacao.setPrecoUnitario(cotacao.preco());
        operacao.setDataHora(LocalDateTime.now());

        operacao = operacaoRepository.save(operacao); // AC-407
        posicaoService.recalcular(carteira, acao);    // AC-402

        return mapper.toResponse(operacao);
    }

    @Transactional
    public OperacaoResponseDTO vender(OperacaoRequestDTO dto) {
        Carteira carteira = carteiraService.buscarAtiva(dto.getCarteiraId()); // AC-309
        Acao acao = buscarAcao(dto.getTicker());

        validarMercado(carteira, acao); // AC-305/AC-403

        // AC-404: não vende mais que a posição atual
        CarteiraAcao posicao = carteiraAcaoRepository
                .findByCarteiraIdAndAcaoId(carteira.getId(), acao.getId())
                .orElseThrow(() -> new BusinessException(
                        "Sem posição em " + dto.getTicker() + " nesta carteira"));

        if (dto.getQuantidade() > posicao.getQuantidade()) {
            throw new BusinessException(
                    "Quantidade excede a posição atual (" + posicao.getQuantidade() + " unidades)");
        }

        CotacaoResultado cotacao = adapterPara(acao.getMercado()).buscarCotacao(acao.getTicker()); // AC-401 (venda também busca no ato)

        Operacao operacao = new Operacao();
        operacao.setCarteira(carteira);
        operacao.setAcao(acao);
        operacao.setTipo(TipoOperacao.VENDA);
        operacao.setQuantidade(dto.getQuantidade());
        operacao.setPrecoUnitario(cotacao.preco());
        operacao.setDataHora(LocalDateTime.now());

        operacao = operacaoRepository.save(operacao); // AC-407
        posicaoService.recalcular(carteira, acao);    // AC-405 (zera posição se necessário)

        return mapper.toResponse(operacao);
    }

    @Transactional
    public OperacaoResponseDTO editar(Long id, Integer novaQuantidade, java.math.BigDecimal novoPreco) {
        Operacao operacao = buscarOperacao(id);

        if (novaQuantidade != null) operacao.setQuantidade(novaQuantidade);
        if (novoPreco != null) operacao.setPrecoUnitario(novoPreco);

        operacao = operacaoRepository.save(operacao);
        posicaoService.recalcular(operacao.getCarteira(), operacao.getAcao()); // AC-412

        return mapper.toResponse(operacao);
    }

    @Transactional
    public void excluir(Long id) {
        Operacao operacao = buscarOperacao(id);
        Carteira carteira = operacao.getCarteira();
        Acao acao = operacao.getAcao();

        operacaoRepository.delete(operacao);
        posicaoService.recalcular(carteira, acao); // AC-413
    }

    private Operacao buscarOperacao(Long id) {
        return operacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Operação não encontrada: " + id));
    }

    private Acao buscarAcao(String ticker) {
        return acaoRepository.findByTicker(ticker.toUpperCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Ação não encontrada: " + ticker));
    }

    private void validarMercado(Carteira carteira, Acao acao) {
        if (carteira.getMercado() != acao.getMercado()) {
            throw new BusinessException(
                    "Incompatibilidade de mercado: carteira é " + carteira.getMercado()
                    + " mas ação " + acao.getTicker() + " é " + acao.getMercado());
        }
    }

    private CotacaoAdapter adapterPara(Mercado mercado) {
        return mercado == Mercado.BR ? brapiAdapter : twelveDataAdapter;
    }
}
