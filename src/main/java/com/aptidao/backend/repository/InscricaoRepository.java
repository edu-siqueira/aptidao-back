package com.aptidao.backend.repository;

import com.aptidao.backend.model.InscricaoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InscricaoRepository extends JpaRepository<InscricaoEntity, Long> {

    List<InscricaoEntity> findAllByOrderByIdAsc();

    Optional<InscricaoEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    long deleteByEmail(String email);
}
