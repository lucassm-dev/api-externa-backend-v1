package com.apiexternabackend.repositories;

import com.apiexternabackend.domains.Corretora;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CorretoraRepository extends JpaRepository<Corretora, Long> {

    boolean existsByCnpjAndAtivoTrue(String cnpj);

    Optional<Corretora> findByCnpjAndAtivoTrue(String cnpj);

    Page<Corretora> findAllByAtivoTrue(Pageable pageable);

    Optional<Corretora> findByIdAndAtivoTrue(Long id);
}
