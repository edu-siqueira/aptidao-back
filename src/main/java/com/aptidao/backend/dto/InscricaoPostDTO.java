package com.aptidao.backend.dto;

import com.aptidao.backend.enums.Percurso;

import java.time.LocalDate;

public record InscricaoPostDTO(String nome, String email, LocalDate dataNascimento, Percurso percurso) {
}
