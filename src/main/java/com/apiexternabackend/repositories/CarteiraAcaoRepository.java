package com.apiexternabackend.repositories;

import com.apiexternabackend.domains.CarteiraAcao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CarteiraAcaoRepository extends JpaRepository<CarteiraAcao, Long> {

    Optional<CarteiraAcao> findByCarteiraIdAndAcaoId(Long carteiraId, Long acaoId);

    List<CarteiraAcao> findByCarteiraId(Long carteiraId);
}
