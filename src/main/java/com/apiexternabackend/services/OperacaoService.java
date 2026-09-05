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
import com.apiexternabackend.resources.exceptions.RecursoNaoEncontradoException;
import com.apiexternabackend.resources.exceptions.RegraVioladaException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    public OperacaoResponseDTO comprar(OperacaoRequestDTO dto, Long investidorId) {
        Carteira carteira = carteiraService.buscarAtiva(dto.getCarteiraId(), investidorId); // AC-309
        Acao acao = buscarAcao(dto.getTicker());

        validarMercado(carteira, acao); // AC-305/AC-403

        CotacaoResultado cotacao = adapterPara(acao.getMercado()).buscarCotacao(acao.getTicker()); // AC-401, sempre buscada (AC-461)
        boolean precoManual = dto.getPrecoUnitario() != null; // AC-457
        BigDecimal precoEfetivo = precoManual ? dto.getPrecoUnitario() : cotacao.preco(); // AC-456
        validarEscalaDecimal(precoEfetivo); // AC-459

        Operacao operacao = new Operacao();
        operacao.setCarteira(carteira);
        operacao.setAcao(acao);
        operacao.setTipo(TipoOperacao.COMPRA);
        operacao.setQuantidade(dto.getQuantidade());
        operacao.setPrecoUnitario(precoEfetivo);
        operacao.setPrecoManual(precoManual);
        operacao.setCotacaoNoMomento(cotacao.preco()); // AC-461
        operacao.setDataHora(LocalDateTime.now());

        operacao = operacaoRepository.save(operacao); // AC-407
        posicaoService.recalcular(carteira, acao);    // AC-402

        return mapper.toResponse(operacao);
    }

    @Transactional
    public OperacaoResponseDTO vender(OperacaoRequestDTO dto, Long investidorId) {
        Carteira carteira = carteiraService.buscarAtiva(dto.getCarteiraId(), investidorId); // AC-309
        Acao acao = buscarAcao(dto.getTicker());

        validarMercado(carteira, acao); // AC-305/AC-403

        // AC-404: não vende mais que a posição atual
        CarteiraAcao posicao = carteiraAcaoRepository
                .findByCarteiraIdAndAcaoId(carteira.getId(), acao.getId())
                .orElseThrow(() -> new RegraVioladaException("OPE-003",
                        "Sem posição em " + dto.getTicker() + " nesta carteira"));

        if (dto.getQuantidade() > posicao.getQuantidade()) {
            throw new RegraVioladaException("OPE-004",
                    "Quantidade excede a posição atual (" + posicao.getQuantidade() + " unidades)");
        }

        CotacaoResultado cotacao = adapterPara(acao.getMercado()).buscarCotacao(acao.getTicker()); // AC-401 (venda também busca no ato), sempre buscada (AC-461)
        boolean precoManual = dto.getPrecoUnitario() != null; // AC-457
        BigDecimal precoEfetivo = precoManual ? dto.getPrecoUnitario() : cotacao.preco(); // AC-456
        validarEscalaDecimal(precoEfetivo); // AC-459

        Operacao operacao = new Operacao();
        operacao.setCarteira(carteira);
        operacao.setAcao(acao);
        operacao.setTipo(TipoOperacao.VENDA);
        operacao.setQuantidade(dto.getQuantidade());
        operacao.setPrecoUnitario(precoEfetivo);
        operacao.setPrecoManual(precoManual);
        operacao.setCotacaoNoMomento(cotacao.preco()); // AC-461
        operacao.setDataHora(LocalDateTime.now());

        operacao = operacaoRepository.save(operacao); // AC-407
        posicaoService.recalcular(carteira, acao);    // AC-405 (zera posição se necessário)

        return mapper.toResponse(operacao);
    }

    @Transactional
    public OperacaoResponseDTO editar(Long id, Integer novaQuantidade, java.math.BigDecimal novoPreco, Long investidorId) {
        Operacao operacao = buscarOperacao(id, investidorId);

        if (novaQuantidade != null) operacao.setQuantidade(novaQuantidade);
        if (novoPreco != null) {
            validarEscalaDecimal(novoPreco); // AC-464
            operacao.setPrecoUnitario(novoPreco);
            operacao.setPrecoManual(true);
            operacao.setCotacaoNoMomento(operacao.getAcao().getCotacaoAtual()); // AC-465, sem nova chamada externa (ASM-419)
        }

        operacao = operacaoRepository.save(operacao);
        posicaoService.recalcular(operacao.getCarteira(), operacao.getAcao()); // AC-412

        return mapper.toResponse(operacao);
    }

    @Transactional
    public void excluir(Long id, Long investidorId) {
        Operacao operacao = buscarOperacao(id, investidorId);
        Carteira carteira = operacao.getCarteira();
        Acao acao = operacao.getAcao();

        operacaoRepository.delete(operacao);
        posicaoService.recalcular(carteira, acao); // AC-413
    }

    private Operacao buscarOperacao(Long id, Long investidorId) {
        Operacao operacao = operacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("OPE-001", "Operação não encontrada: " + id));
        if (!operacao.getCarteira().getInvestidor().getId().equals(investidorId)) {
            // não revela que a operação existe e é de outro investidor (mesma semântica de CAR-001/ASM-411)
            throw new RecursoNaoEncontradoException("OPE-001", "Operação não encontrada: " + id);
        }
        return operacao;
    }

    private Acao buscarAcao(String ticker) {
        return acaoRepository.findByTickerAndAtivoTrue(ticker.toUpperCase().trim())
                .orElseThrow(() -> new RecursoNaoEncontradoException("ACA-001", "Ação não encontrada: " + ticker));
    }

    private void validarMercado(Carteira carteira, Acao acao) {
        if (carteira.getMercado() != acao.getMercado()) {
            throw new RegraVioladaException("OPE-002",
                    "Incompatibilidade de mercado: carteira é " + carteira.getMercado()
                    + " mas ação " + acao.getTicker() + " é " + acao.getMercado());
        }
    }

    private void validarEscalaDecimal(BigDecimal preco) {
        if (preco.stripTrailingZeros().scale() > 2) {
            throw new RegraVioladaException("OPE-005",
                    "Preço unitário deve ter no máximo 2 casas decimais: " + preco);
        }
    }

    private CotacaoAdapter adapterPara(Mercado mercado) {
        return mercado == Mercado.BR ? brapiAdapter : twelveDataAdapter;
    }
}
