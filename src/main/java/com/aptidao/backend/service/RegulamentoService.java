package com.aptidao.backend.service;

import com.aptidao.backend.enums.EnumCategoria;
import com.aptidao.backend.enums.EnumPercurso;
import com.aptidao.backend.exceptions.RegraNegocioException;

import java.time.LocalDate;
import java.time.Period;

public class RegulamentoService {

    private static final int IDADE_MINIMA = 14;
    private static final int IDADE_MINIMA_KM_10 = 18;
    private static final int IDADE_INICIO_ADULTO = 18;
    private static final int IDADE_INICIO_MASTER = 40;
    private static final int IDADE_INICIO_SENIOR = 60;

    private final LocalDate dataProva;

    public RegulamentoService(LocalDate dataProva) {
        this.dataProva = dataProva;
    }

    public int calcularIdadeNaProva(LocalDate dataNascimento) {
        return Period.between(dataNascimento, dataProva).getYears();
    }

    public void validarParticipacao(int idade, EnumPercurso percurso) {
        if (idade < IDADE_MINIMA) {
            throw new RegraNegocioException("A idade mínima para participar é " + IDADE_MINIMA + " anos.");
        }
        if (percurso == EnumPercurso.KM_10 && idade < IDADE_MINIMA_KM_10) {
            throw new RegraNegocioException(
                    "O percurso de 10 km é permitido somente a partir de " + IDADE_MINIMA_KM_10 + " anos.");
        }
    }

    public EnumCategoria definirCategoria(int idade) {
        if (idade >= IDADE_INICIO_SENIOR) {
            return EnumCategoria.SENIOR;
        }
        if (idade >= IDADE_INICIO_MASTER) {
            return EnumCategoria.MASTER;
        }
        if (idade >= IDADE_INICIO_ADULTO) {
            return EnumCategoria.ADULTO;
        }
        return EnumCategoria.JUVENIL;
    }
}
