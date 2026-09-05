package com.apiexternabackend.repositories;

import com.apiexternabackend.domains.Carteira;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarteiraRepository extends JpaRepository<Carteira, Long> {

    Page<Carteira> findByInvestidorIdAndAtivaTrue(Long investidorId, Pageable pageable);

    long countByCorretoraIdAndAtivaTrue(Long corretoraId);
}
