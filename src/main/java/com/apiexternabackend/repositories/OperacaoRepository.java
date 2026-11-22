package com.apiexternabackend.repositories;

import com.apiexternabackend.domains.Operacao;
import com.apiexternabackend.domains.enums.TipoOperacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OperacaoRepository extends JpaRepository<Operacao, Long> {

    @Query("""
            SELECT o FROM Operacao o WHERE o.carteira.investidor.id = :investidorId AND o.ativo = true
            AND (CAST(:inicio AS LocalDateTime) IS NULL OR o.dataHora >= :inicio)
            AND (CAST(:fimExclusivo AS LocalDateTime) IS NULL OR o.dataHora < :fimExclusivo)
            AND (CAST(:ticker AS String) IS NULL OR UPPER(o.acao.ticker) = :ticker)
            AND (CAST(:carteiraId AS Long) IS NULL OR o.carteira.id = :carteiraId)
            AND (CAST(:tipo AS String) IS NULL OR o.tipo = :tipo)
            AND (CAST(:lucro AS Boolean) IS NULL OR (:lucro = true AND o.lucroRealizado > 0)
                 OR (:lucro = false AND o.lucroRealizado < 0))
            ORDER BY o.dataHora DESC""")
    Page<Operacao> buscarExtrato(@Param("investidorId") Long investidorId, @Param("inicio") LocalDateTime inicio,
                                 @Param("fimExclusivo") LocalDateTime fimExclusivo, @Param("ticker") String ticker,
                                 @Param("carteiraId") Long carteiraId, @Param("tipo") TipoOperacao tipo,
                                 @Param("lucro") Boolean lucro, Pageable pageable);

    List<Operacao> findByCarteiraIdAndAcaoIdAndAtivoTrueOrderByDataHoraAsc(Long carteiraId, Long acaoId);

    Optional<Operacao> findByIdAndAtivoTrue(Long id);

    List<Operacao> findByCarteiraIdAndTipoAndAtivoTrue(Long carteiraId, TipoOperacao tipo);
}
