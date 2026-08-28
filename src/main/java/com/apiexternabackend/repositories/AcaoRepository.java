package com.apiexternabackend.repositories;

import com.apiexternabackend.domains.Acao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AcaoRepository extends JpaRepository<Acao, Long> {

    boolean existsByTicker(String ticker);

    Optional<Acao> findByTicker(String ticker);

    Page<Acao> findAllByAtivoTrue(Pageable pageable);

    Optional<Acao> findByTickerAndAtivoTrue(String ticker);
}
