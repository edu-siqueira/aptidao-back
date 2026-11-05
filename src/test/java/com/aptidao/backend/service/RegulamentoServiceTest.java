package com.aptidao.backend.service;

import com.aptidao.backend.enums.Categoria;
import com.aptidao.backend.enums.Percurso;
import com.aptidao.backend.exceptions.RegraNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegulamentoServiceTest {

    private static final LocalDate DATA_PROVA = LocalDate.of(2027, 3, 14);
    private static final String MENSAGEM_IDADE_MINIMA = "A idade mínima para participar é 14 anos.";
    private static final String MENSAGEM_KM_10 = "O percurso de 10 km é permitido somente a partir de 18 anos.";

    private final RegulamentoService regulamento = new RegulamentoService(DATA_PROVA);

    @ParameterizedTest
    @CsvSource({
            "2013-03-13, 14",
            "2013-03-14, 14",
            "2013-03-15, 13",
            "2009-03-13, 18",
            "2009-03-14, 18",
            "2009-03-15, 17",
            "1987-03-14, 40",
            "1987-03-15, 39",
            "1967-03-14, 60",
            "1967-03-15, 59"
    })
    void calculaIdadeCompletaNaDataDaProva(LocalDate dataNascimento, int idadeEsperada) {
        assertThat(regulamento.calcularIdadeNaProva(dataNascimento)).isEqualTo(idadeEsperada);
    }

    @ParameterizedTest
    @CsvSource({
            "14, JUVENIL",
            "17, JUVENIL",
            "18, ADULTO",
            "39, ADULTO",
            "40, MASTER",
            "59, MASTER",
            "60, SENIOR",
            "90, SENIOR"
    })
    void defineCategoriaNasBordas(int idade, Categoria categoriaEsperada) {
        assertThat(regulamento.definirCategoria(idade)).isEqualTo(categoriaEsperada);
    }

    @ParameterizedTest
    @EnumSource(Percurso.class)
    void recusaMenorDe14AnosEmQualquerPercurso(Percurso percurso) {
        assertThatThrownBy(() -> regulamento.validarParticipacao(13, percurso))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage(MENSAGEM_IDADE_MINIMA);
    }

    @Test
    void idadeMinimaTemPrecedenciaSobreRestricaoDosDezKm() {
        assertThatThrownBy(() -> regulamento.validarParticipacao(13, Percurso.KM_10))
                .hasMessage(MENSAGEM_IDADE_MINIMA);
    }

    @Test
    void recusaDezKmParaMenorDe18Anos() {
        assertThatThrownBy(() -> regulamento.validarParticipacao(17, Percurso.KM_10))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage(MENSAGEM_KM_10);
    }

    @Test
    void aceitaCincoKmAPartirDe14Anos() {
        assertThatCode(() -> regulamento.validarParticipacao(14, Percurso.KM_5)).doesNotThrowAnyException();
        assertThatCode(() -> regulamento.validarParticipacao(17, Percurso.KM_5)).doesNotThrowAnyException();
    }

    @Test
    void aceitaDezKmAPartirDe18Anos() {
        assertThatCode(() -> regulamento.validarParticipacao(18, Percurso.KM_10)).doesNotThrowAnyException();
    }
}
