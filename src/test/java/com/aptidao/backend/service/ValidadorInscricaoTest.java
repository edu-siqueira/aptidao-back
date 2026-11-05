package com.aptidao.backend.service;

import com.aptidao.backend.enums.Percurso;
import com.aptidao.backend.exceptions.RegraNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidadorInscricaoTest {

    private static final String MENSAGEM_NOME_TAMANHO = "O nome deve ter entre 3 e 100 caracteres.";
    private static final String MENSAGEM_EMAIL_INVALIDO = "O e-mail informado é inválido.";

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-05T12:00:00Z"), ZoneOffset.UTC);
    private final ValidadorInscricao validador = new ValidadorInscricao(clock);

    @Test
    void normalizaNomeRemovendoEspacosDasPontasEReduzindoOsInternos() {
        assertThat(validador.normalizarNome("  Ana   Souza  ")).isEqualTo("Ana Souza");
    }

    @Test
    void normalizaNomePreservandoAcentosApostrofosEHifens() {
        assertThat(validador.normalizarNome(" João   D'Ávila-Silva ")).isEqualTo("João D'Ávila-Silva");
    }

    @Test
    void normalizaEmailRemovendoEspacosDasPontasEConvertendoParaMinusculas() {
        assertThat(validador.normalizarEmail("  Ana.Souza@Email.COM ")).isEqualTo("ana.souza@email.com");
    }

    @Test
    void normalizacaoDeValorAusenteDevolveNulo() {
        assertThat(validador.normalizarNome(null)).isNull();
        assertThat(validador.normalizarEmail(null)).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void recusaNomeAusente(String nome) {
        assertThatThrownBy(() -> validador.validarNome(nome))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O nome é obrigatório.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "ab"})
    void recusaNomeComMenosDeTresCaracteres(String nome) {
        assertThatThrownBy(() -> validador.validarNome(nome)).hasMessage(MENSAGEM_NOME_TAMANHO);
    }

    @Test
    void recusaNomeComMaisDeCemCaracteres() {
        assertThatThrownBy(() -> validador.validarNome("a".repeat(101))).hasMessage(MENSAGEM_NOME_TAMANHO);
    }

    @Test
    void aceitaNomeNasBordasDeTamanho() {
        assertThatCode(() -> validador.validarNome("Ana")).doesNotThrowAnyException();
        assertThatCode(() -> validador.validarNome("a".repeat(100))).doesNotThrowAnyException();
    }

    @Test
    void tamanhoDoNomeConsideraOValorNormalizado() {
        String nomeNormalizado = validador.normalizarNome("  ab  ");

        assertThatThrownBy(() -> validador.validarNome(nomeNormalizado)).hasMessage(MENSAGEM_NOME_TAMANHO);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void recusaEmailAusente(String email) {
        assertThatThrownBy(() -> validador.validarEmail(email))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O e-mail é obrigatório.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ana", "ana@", "@email.com", "ana@email", "ana@@email.com", "ana souza@email.com",
            "ana@email..com", "ana@.email.com", "ana@email.com."})
    void recusaEmailForaDoFormato(String email) {
        assertThatThrownBy(() -> validador.validarEmail(email)).hasMessage(MENSAGEM_EMAIL_INVALIDO);
    }

    @Test
    void recusaEmailMaiorQueOLimite() {
        String email = "a".repeat(250) + "@b.co";

        assertThatThrownBy(() -> validador.validarEmail(email)).hasMessage(MENSAGEM_EMAIL_INVALIDO);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ana@email.com", "ana.souza+corrida@mail.com.br", "joão@email.com", "a-b_c@sub.dominio.org"})
    void aceitaEmailValido(String email) {
        assertThatCode(() -> validador.validarEmail(email)).doesNotThrowAnyException();
    }

    @Test
    void recusaDataDeNascimentoAusente() {
        assertThatThrownBy(() -> validador.validarDataNascimento(null))
                .hasMessage("A data de nascimento é obrigatória.");
    }

    @Test
    void aceitaDataDeNascimentoAteOntem() {
        assertThatCode(() -> validador.validarDataNascimento(LocalDate.of(2026, 11, 4))).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-11-05", "2026-11-06", "2030-01-01"})
    void recusaDataDeNascimentoDeHojeEmDiante(String data) {
        assertThatThrownBy(() -> validador.validarDataNascimento(LocalDate.parse(data)))
                .hasMessage("A data de nascimento deve ser anterior à data de hoje.");
    }

    @Test
    void recusaPercursoAusente() {
        assertThatThrownBy(() -> validador.validarPercurso(null))
                .hasMessage("O percurso é obrigatório. Use KM_5 ou KM_10.");
        assertThatCode(() -> validador.validarPercurso(Percurso.KM_5)).doesNotThrowAnyException();
    }
}
