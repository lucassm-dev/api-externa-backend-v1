package com.apiexternabackend.services;

import com.apiexternabackend.domains.Acao;
import com.apiexternabackend.domains.Carteira;
import com.apiexternabackend.domains.CarteiraAcao;
import com.apiexternabackend.domains.Operacao;
import com.apiexternabackend.domains.enums.TipoOperacao;
import com.apiexternabackend.repositories.CarteiraAcaoRepository;
import com.apiexternabackend.repositories.OperacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Calcula e mantém as posições (quantidade + preço médio) derivadas do histórico
 * de movimentações. A posição é o agregado — a operação é a fonte da verdade.
 */
@Service
@RequiredArgsConstructor
public class PosicaoService {

    private final CarteiraAcaoRepository carteiraAcaoRepository;
    private final OperacaoRepository operacaoRepository;

    /**
     * Recalcula a posição de um par carteira/ação a partir do histórico ativo completo,
     * regravando também o lucro realizado de cada venda (fonte única de verdade —
     * é o que propaga em cascata quando uma compra anterior é editada/excluída).
     * Chamado após qualquer insert, update ou soft delete de Operacao.
     */
    public void recalcular(Carteira carteira, Acao acao) {
        List<Operacao> historico = operacaoRepository
                .findByCarteiraIdAndAcaoIdAndAtivoTrueOrderByDataHoraAsc(carteira.getId(), acao.getId());

        int quantidade = 0;
        BigDecimal custoTotal = BigDecimal.ZERO;

        for (Operacao op : historico) {
            if (op.getTipo() == TipoOperacao.COMPRA) {
                // Preço médio ponderado: soma os custos (RN-P02)
                custoTotal = custoTotal.add(op.getPrecoUnitario().multiply(BigDecimal.valueOf(op.getQuantidade())));
                quantidade += op.getQuantidade();
            } else {
                // Venda — reduz pela quantidade vendida (custo proporcional ao PM atual)
                BigDecimal custoUnitario = quantidade > 0
                        ? custoTotal.divide(BigDecimal.valueOf(quantidade), 10, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                // AC-469/AC-474/AC-475/AC-476: lucro realizado é regravado a cada recálculo,
                // usando o preço médio de compra vigente NESTE ponto da série ativa (ASM-423)
                op.setPrecoMedioCompraNoMomento(custoUnitario.setScale(4, RoundingMode.HALF_UP));
                op.setLucroRealizado(op.getPrecoUnitario().subtract(custoUnitario)
                        .multiply(BigDecimal.valueOf(op.getQuantidade())).setScale(4, RoundingMode.HALF_UP));
                operacaoRepository.save(op);

                custoTotal = custoTotal.subtract(custoUnitario.multiply(BigDecimal.valueOf(op.getQuantidade())));
                quantidade -= op.getQuantidade();
            }
        }

        Optional<CarteiraAcao> posicaoExistente = carteiraAcaoRepository
                .findByCarteiraIdAndAcaoId(carteira.getId(), acao.getId());

        if (quantidade <= 0) {
            // RN-P04: venda que zera a posição a remove
            posicaoExistente.ifPresent(carteiraAcaoRepository::delete);
            return;
        }

        BigDecimal precoMedio = quantidade > 0
                ? custoTotal.divide(BigDecimal.valueOf(quantidade), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        CarteiraAcao posicao = posicaoExistente.orElseGet(() -> {
            CarteiraAcao nova = new CarteiraAcao();
            nova.setCarteira(carteira);
            nova.setAcao(acao);
            return nova;
        });

        posicao.setQuantidade(quantidade);
        posicao.setPrecoMedio(precoMedio);
        carteiraAcaoRepository.save(posicao);
    }
}