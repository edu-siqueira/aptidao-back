package com.aptidao.backend.dto;

import com.aptidao.backend.enums.Percurso;

import java.time.LocalDate;

public record InscricaoPutDTO(String nome, LocalDate dataNascimento, Percurso percurso) {
}
