package com.apiexternabackend.repositories;

import com.apiexternabackend.domains.Operacao;
import com.apiexternabackend.domains.enums.TipoOperacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OperacaoRepository extends JpaRepository<Operacao, Long> {

    @Query("SELECT o FROM Operacao o WHERE o.carteira.investidor.id = :investidorId AND o.ativo = true ORDER BY o.dataHora DESC")
    Page<Operacao> findByInvestidorId(@Param("investidorId") Long investidorId, Pageable pageable);

    List<Operacao> findByCarteiraIdAndAcaoIdAndAtivoTrueOrderByDataHoraAsc(Long carteiraId, Long acaoId);

    Optional<Operacao> findByIdAndAtivoTrue(Long id);

    List<Operacao> findByCarteiraIdAndTipoAndAtivoTrue(Long carteiraId, TipoOperacao tipo);
}
