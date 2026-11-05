package com.aptidao.backend.repository;

import com.aptidao.backend.model.Inscricao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InscricaoRepository extends JpaRepository<Inscricao, Long> {

    List<Inscricao> findAllByOrderByIdAsc();

    Optional<Inscricao> findByEmail(String email);

    boolean existsByEmail(String email);

    long deleteByEmail(String email);
}
