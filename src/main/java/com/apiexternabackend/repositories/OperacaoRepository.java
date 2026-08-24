package com.apiexternabackend.repositories;

import com.apiexternabackend.domains.Operacao;
import com.apiexternabackend.domains.enums.TipoOperacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OperacaoRepository extends JpaRepository<Operacao, Long> {

    @Query("SELECT o FROM Operacao o WHERE o.carteira.investidor.id = :investidorId ORDER BY o.dataHora DESC")
    Page<Operacao> findByInvestidorId(@Param("investidorId") Long investidorId, Pageable pageable);

    List<Operacao> findByCarteiraIdAndAcaoIdOrderByDataHoraAsc(Long carteiraId, Long acaoId);
}
