package com.apiexternabackend.repositories;

import com.apiexternabackend.domains.Investidor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvestidorRepository extends JpaRepository<Investidor, Long> {

    boolean existsByEmail(String email);

    boolean existsByCpf(String cpf);

    Page<Investidor> findAllByAtivoTrue(Pageable pageable);

    Optional<Investidor> findByIdAndAtivoTrue(Long id);
}
