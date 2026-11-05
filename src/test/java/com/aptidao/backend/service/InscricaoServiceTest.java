package com.aptidao.backend.service;

import com.aptidao.backend.dto.InscricaoPutDTO;
import com.aptidao.backend.dto.InscricaoPostDTO;
import com.aptidao.backend.dto.InscricaoDTO;
import com.aptidao.backend.dto.InscricaoResumoDTO;
import com.aptidao.backend.enums.Categoria;
import com.aptidao.backend.enums.Percurso;
import com.aptidao.backend.exceptions.InscricaoDuplicadaException;
import com.aptidao.backend.exceptions.InscricaoNaoEncontradaException;
import com.aptidao.backend.exceptions.RegraNegocioException;
import com.aptidao.backend.repository.InscricaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class InscricaoServiceTest {

    private static final LocalDate NASCIMENTO_ADULTO = LocalDate.of(1990, 5, 20);

    @Autowired
    private InscricaoService service;

    @Autowired
    private InscricaoRepository repository;

    @BeforeEach
    void limparBanco() {
        repository.deleteAll();
    }

    @Test
    void cadastraNormalizandoDadosECalculandoIdadeECategoria() {
        InscricaoDTO resposta = service.cadastrar(
                cadastro("  Ana   Souza ", " Ana.Souza@Email.COM ", NASCIMENTO_ADULTO, Percurso.KM_10));

        assertThat(resposta).isEqualTo(new InscricaoDTO(
                "Ana Souza", "ana.souza@email.com", NASCIMENTO_ADULTO, Percurso.KM_10, 36, Categoria.ADULTO));
    }

    @Test
    void listaNaOrdemDeCadastro() {
        service.cadastrar(cadastro("Bruno", "bruno@email.com", NASCIMENTO_ADULTO, Percurso.KM_5));
        service.cadastrar(cadastro("Ana", "ana@email.com", NASCIMENTO_ADULTO, Percurso.KM_5));
        service.cadastrar(cadastro("Carla", "carla@email.com", NASCIMENTO_ADULTO, Percurso.KM_5));

        assertThat(service.listar()).extracting(InscricaoDTO::email)
                .containsExactly("bruno@email.com", "ana@email.com", "carla@email.com");
    }

    @Test
    void recusaEmailRepetidoMesmoComCaixaEEspacosDiferentes() {
        service.cadastrar(cadastro("Ana Souza", "ana@email.com", NASCIMENTO_ADULTO, Percurso.KM_5));

        assertThatThrownBy(() -> service.cadastrar(
                cadastro("Outra Ana", " Ana@Email.COM ", NASCIMENTO_ADULTO, Percurso.KM_10)))
                .isInstanceOf(InscricaoDuplicadaException.class);
        assertThat(service.listar()).hasSize(1);
    }

    @Test
    void recusaMenorDe14AnosNaDataDaProva() {
        assertThatThrownBy(() -> service.cadastrar(
                cadastro("Julia Lima", "julia@email.com", LocalDate.of(2013, 3, 15), Percurso.KM_5)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("A idade mínima para participar é 14 anos.");
    }

    @Test
    void idadeMinimaTemPrecedenciaSobreRestricaoDosDezKm() {
        assertThatThrownBy(() -> service.cadastrar(
                cadastro("Julia Lima", "julia@email.com", LocalDate.of(2013, 3, 15), Percurso.KM_10)))
                .hasMessage("A idade mínima para participar é 14 anos.");
    }

    @Test
    void aceitaQuemFaz14AnosNoDiaDaProva() {
        InscricaoDTO resposta = service.cadastrar(
                cadastro("Pedro Lima", "pedro@email.com", LocalDate.of(2013, 3, 14), Percurso.KM_5));

        assertThat(resposta.idadeNaProva()).isEqualTo(14);
        assertThat(resposta.categoria()).isEqualTo(Categoria.JUVENIL);
    }

    @Test
    void recusaDezKmParaQuemFaz18AnosDepoisDaProva() {
        assertThatThrownBy(() -> service.cadastrar(
                cadastro("Pedro Lima", "pedro@email.com", LocalDate.of(2009, 3, 15), Percurso.KM_10)))
                .hasMessage("O percurso de 10 km é permitido somente a partir de 18 anos.");
    }

    @Test
    void aceitaDezKmParaQuemFaz18AnosNoDiaDaProva() {
        InscricaoDTO resposta = service.cadastrar(
                cadastro("Pedro Lima", "pedro@email.com", LocalDate.of(2009, 3, 14), Percurso.KM_10));

        assertThat(resposta.idadeNaProva()).isEqualTo(18);
        assertThat(resposta.categoria()).isEqualTo(Categoria.ADULTO);
    }

    @Test
    void recusaDataDeNascimentoFutura() {
        assertThatThrownBy(() -> service.cadastrar(
                cadastro("Bebe Lima", "bebe@email.com", LocalDate.now().plusDays(1), Percurso.KM_5)))
                .hasMessage("A data de nascimento deve ser anterior à data de hoje.");
    }

    @Test
    void recusaCamposAusentesComMensagemDoPrimeiroProblema() {
        assertThatThrownBy(() -> service.cadastrar(new InscricaoPostDTO(null, null, null, null)))
                .hasMessage("O e-mail é obrigatório.");
        assertThatThrownBy(() -> service.cadastrar(new InscricaoPostDTO(null, "ana@email.com", null, null)))
                .hasMessage("O nome é obrigatório.");
        assertThatThrownBy(() -> service.cadastrar(new InscricaoPostDTO("Ana Souza", "ana@email.com", null, null)))
                .hasMessage("A data de nascimento é obrigatória.");
        assertThatThrownBy(() -> service.cadastrar(
                new InscricaoPostDTO("Ana Souza", "ana@email.com", NASCIMENTO_ADULTO, null)))
                .hasMessage("O percurso é obrigatório. Use KM_5 ou KM_10.");
        assertThat(service.listar()).isEmpty();
    }

    @Test
    void consultaPeloEmailComCaixaEEspacosDiferentes() {
        service.cadastrar(cadastro("Ana Souza", "ana@email.com", NASCIMENTO_ADULTO, Percurso.KM_5));

        assertThat(service.consultar(" ANA@email.com ").nome()).isEqualTo("Ana Souza");
    }

    @Test
    void consultaDeInscricaoAusenteFalha() {
        assertThatThrownBy(() -> service.consultar("ninguem@email.com"))
                .isInstanceOf(InscricaoNaoEncontradaException.class);
    }

    @Test
    void editaMantendoEmailPosicaoETotalERecalculandoDerivados() {
        service.cadastrar(cadastro("Bruno Alves", "bruno@email.com", NASCIMENTO_ADULTO, Percurso.KM_5));
        service.cadastrar(cadastro("Ana Souza", "ana@email.com", NASCIMENTO_ADULTO, Percurso.KM_5));

        InscricaoDTO editada = service.editar(" BRUNO@email.com ",
                new InscricaoPutDTO("  Bruno   Alves Filho ", LocalDate.of(1960, 3, 14), Percurso.KM_10));

        assertThat(editada).isEqualTo(new InscricaoDTO("Bruno Alves Filho", "bruno@email.com",
                LocalDate.of(1960, 3, 14), Percurso.KM_10, 67, Categoria.SENIOR));
        assertThat(service.listar()).extracting(InscricaoDTO::email)
                .containsExactly("bruno@email.com", "ana@email.com");
        assertThat(service.resumo()).isEqualTo(new InscricaoResumoDTO(2, 1, 1));
    }

    @Test
    void edicaoRecusadaPreservaDadosETotaisAnteriores() {
        InscricaoDTO original = service.cadastrar(
                cadastro("Ana Souza", "ana@email.com", NASCIMENTO_ADULTO, Percurso.KM_5));

        assertThatThrownBy(() -> service.editar("ana@email.com",
                new InscricaoPutDTO("Ana Nova", LocalDate.of(2009, 3, 15), Percurso.KM_10)))
                .isInstanceOf(RegraNegocioException.class);

        assertThat(service.consultar("ana@email.com")).isEqualTo(original);
        assertThat(service.resumo()).isEqualTo(new InscricaoResumoDTO(1, 1, 0));
    }

    @Test
    void edicaoComNomeInvalidoPreservaOsDados() {
        InscricaoDTO original = service.cadastrar(
                cadastro("Ana Souza", "ana@email.com", NASCIMENTO_ADULTO, Percurso.KM_5));

        assertThatThrownBy(() -> service.editar("ana@email.com",
                new InscricaoPutDTO(" a ", NASCIMENTO_ADULTO, Percurso.KM_10)))
                .hasMessage("O nome deve ter entre 3 e 100 caracteres.");

        assertThat(service.consultar("ana@email.com")).isEqualTo(original);
    }

    @Test
    void edicaoDeInscricaoAusenteFalha() {
        assertThatThrownBy(() -> service.editar("ninguem@email.com",
                new InscricaoPutDTO("Ana Souza", NASCIMENTO_ADULTO, Percurso.KM_5)))
                .isInstanceOf(InscricaoNaoEncontradaException.class);
    }

    @Test
    void cancelaLiberaOEmailERecadastraNoFimDaLista() {
        service.cadastrar(cadastro("Ana Souza", "ana@email.com", NASCIMENTO_ADULTO, Percurso.KM_5));
        service.cadastrar(cadastro("Bruno Alves", "bruno@email.com", NASCIMENTO_ADULTO, Percurso.KM_5));

        service.cancelar(" ANA@email.com ");
        service.cadastrar(cadastro("Ana Nova", "ana@email.com", NASCIMENTO_ADULTO, Percurso.KM_10));

        assertThat(service.listar()).extracting(InscricaoDTO::nome).containsExactly("Bruno Alves", "Ana Nova");
    }

    @Test
    void cancelarAusenteFalhaSemAlterarOsDemais() {
        service.cadastrar(cadastro("Ana Souza", "ana@email.com", NASCIMENTO_ADULTO, Percurso.KM_5));

        assertThatThrownBy(() -> service.cancelar("ninguem@email.com"))
                .isInstanceOf(InscricaoNaoEncontradaException.class);
        assertThat(service.listar()).hasSize(1);
    }

    @Test
    void resumoVazioTemTodosOsTotaisZerados() {
        assertThat(service.resumo()).isEqualTo(new InscricaoResumoDTO(0, 0, 0));
    }

    @Test
    void resumoAcompanhaCadastroEdicaoECancelamento() {
        service.cadastrar(cadastro("Ana Souza", "ana@email.com", NASCIMENTO_ADULTO, Percurso.KM_5));
        service.cadastrar(cadastro("Bruno Alves", "bruno@email.com", NASCIMENTO_ADULTO, Percurso.KM_5));
        service.cadastrar(cadastro("Carla Dias", "carla@email.com", NASCIMENTO_ADULTO, Percurso.KM_10));
        assertThat(service.resumo()).isEqualTo(new InscricaoResumoDTO(3, 2, 1));

        service.editar("ana@email.com", new InscricaoPutDTO("Ana Souza", NASCIMENTO_ADULTO, Percurso.KM_10));
        assertThat(service.resumo()).isEqualTo(new InscricaoResumoDTO(3, 1, 2));

        service.cancelar("carla@email.com");
        assertThat(service.resumo()).isEqualTo(new InscricaoResumoDTO(2, 1, 1));
    }

    private InscricaoPostDTO cadastro(String nome, String email, LocalDate nascimento, Percurso percurso) {
        return new InscricaoPostDTO(nome, email, nascimento, percurso);
    }
}
