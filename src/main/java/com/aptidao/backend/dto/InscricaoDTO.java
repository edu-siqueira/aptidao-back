package com.aptidao.backend.dto;

import com.aptidao.backend.enums.Categoria;
import com.aptidao.backend.enums.Percurso;

import java.time.LocalDate;

public record InscricaoDTO(
        String nome,
        String email,
        LocalDate dataNascimento,
        Percurso percurso,
        int idadeNaProva,
        Categoria categoria) {
}
