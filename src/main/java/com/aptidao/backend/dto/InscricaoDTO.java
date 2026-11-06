package com.aptidao.backend.dto;

import com.aptidao.backend.enums.EnumCategoria;
import com.aptidao.backend.enums.EnumPercurso;

import java.time.LocalDate;

public record InscricaoDTO(
        String nome,
        String email,
        LocalDate dataNascimento,
        EnumPercurso percurso,
        int idadeNaProva,
        EnumCategoria categoria) {
}
