package com.aptidao.backend.dto;

import com.aptidao.backend.enums.EnumPercurso;

import java.time.LocalDate;

public record InscricaoPutDTO(String nome, LocalDate dataNascimento, EnumPercurso percurso) {
}
