package com.aptidao.backend.model;

import com.aptidao.backend.enums.EnumPercurso;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "inscricao")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InscricaoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Setter
    @Column(nullable = false)
    private LocalDate dataNascimento;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnumPercurso percurso;

    public InscricaoEntity(String nome, String email, LocalDate dataNascimento, EnumPercurso percurso) {
        this.nome = nome;
        this.email = email;
        this.dataNascimento = dataNascimento;
        this.percurso = percurso;
    }
}
