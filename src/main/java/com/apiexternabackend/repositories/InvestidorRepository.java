package com.apiexternabackend.repositories;

import com.apiexternabackend.domains.Investidor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestidorRepository extends JpaRepository<Investidor, Long> {

    boolean existsByEmail(String email);

    boolean existsByCpf(String cpf);
}
