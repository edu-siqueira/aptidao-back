package com.aptidao.backend.repository;

import com.aptidao.backend.enums.Percurso;
import com.aptidao.backend.model.Inscricao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
class InscricaoRepositoryTest {

    @Autowired
    private InscricaoRepository repository;

    @Test
    void listaNaOrdemDeCadastro() {
        repository.saveAndFlush(inscricao("Bruno", "bruno@email.com"));
        repository.saveAndFlush(inscricao("Ana", "ana@email.com"));
        repository.saveAndFlush(inscricao("Carla", "carla@email.com"));

        assertThat(repository.findAllByOrderByIdAsc())
                .extracting(Inscricao::getEmail)
                .containsExactly("bruno@email.com", "ana@email.com", "carla@email.com");
    }

    @Test
    void editarMantemAPosicaoNaLista() {
        Inscricao primeira = repository.saveAndFlush(inscricao("Bruno", "bruno@email.com"));
        repository.saveAndFlush(inscricao("Ana", "ana@email.com"));

        primeira.setNome("Bruno Alterado");
        primeira.setPercurso(Percurso.KM_10);
        repository.saveAndFlush(primeira);

        assertThat(repository.findAllByOrderByIdAsc())
                .extracting(Inscricao::getEmail, Inscricao::getNome, Inscricao::getPercurso)
                .containsExactly(
                        tuple("bruno@email.com", "Bruno Alterado", Percurso.KM_10),
                        tuple("ana@email.com", "Ana", Percurso.KM_5));
    }

    @Test
    void bancoRecusaEmailRepetido() {
        repository.saveAndFlush(inscricao("Ana", "ana@email.com"));

        assertThatThrownBy(() -> repository.saveAndFlush(inscricao("Outra Ana", "ana@email.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void cancelarLiberaOEmailEORecadastroVaiParaOFim() {
        repository.saveAndFlush(inscricao("Ana", "ana@email.com"));
        repository.saveAndFlush(inscricao("Bruno", "bruno@email.com"));

        long removidas = repository.deleteByEmail("ana@email.com");
        repository.flush();
        repository.saveAndFlush(inscricao("Ana Nova", "ana@email.com"));

        assertThat(removidas).isEqualTo(1);
        assertThat(repository.findAllByOrderByIdAsc())
                .extracting(Inscricao::getNome)
                .containsExactly("Bruno", "Ana Nova");
    }

    @Test
    void cancelarAusenteNaoAlteraOsDemais() {
        repository.saveAndFlush(inscricao("Ana", "ana@email.com"));

        long removidas = repository.deleteByEmail("ninguem@email.com");

        assertThat(removidas).isZero();
        assertThat(repository.findAllByOrderByIdAsc()).hasSize(1);
    }

    private Inscricao inscricao(String nome, String email) {
        return new Inscricao(nome, email, LocalDate.of(1990, 5, 20), Percurso.KM_5);
    }
}
