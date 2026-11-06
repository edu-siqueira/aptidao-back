package com.aptidao.backend.controller;

import com.aptidao.backend.repository.InscricaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InscricaoControllerTest {

    private static final String ROTA = "/api/inscricoes";
    private static final String ROTA_POR_EMAIL = ROTA + "/{email}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InscricaoRepository repository;

    @BeforeEach
    void limparBanco() {
        repository.deleteAll();
    }

    @Test
    void cadastraEDevolve201ComOsSeisCamposCalculados() throws Exception {
        cadastrar("Ana Souza", "ana.souza@email.com", "1990-05-20", "KM_10")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$.nome").value("Ana Souza"))
                .andExpect(jsonPath("$.email").value("ana.souza@email.com"))
                .andExpect(jsonPath("$.dataNascimento").value("1990-05-20"))
                .andExpect(jsonPath("$.percurso").value("KM_10"))
                .andExpect(jsonPath("$.idadeNaProva").value(36))
                .andExpect(jsonPath("$.categoria").value("ADULTO"));
    }

    @Test
    void cadastroNormalizaNomeEEmail() throws Exception {
        cadastrar("  Ana   Souza ", " Ana.Souza@Email.COM ", "1990-05-20", "KM_5")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Ana Souza"))
                .andExpect(jsonPath("$.email").value("ana.souza@email.com"));
    }

    @Test
    void cadastroIgnoraIdadeECategoriaEnviadasPeloCliente() throws Exception {
        String corpo = """
                {"nome": "Ana Souza", "email": "ana@email.com", "dataNascimento": "1990-05-20",
                 "percurso": "KM_5", "idadeNaProva": 99, "categoria": "SENIOR"}""";

        mockMvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idadeNaProva").value(36))
                .andExpect(jsonPath("$.categoria").value("ADULTO"));
    }

    @Test
    void cadastroRecusaEmailRepetidoMesmoComCaixaEEspacosDiferentes() throws Exception {
        cadastrar("Ana Souza", "ana@email.com", "1990-05-20", "KM_5").andExpect(status().isCreated());

        cadastrar("Outra Ana", " Ana@Email.COM ", "1992-01-10", "KM_10")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("O e-mail informado já possui uma inscrição ativa."));

        mockMvc.perform(get(ROTA)).andExpect(jsonPath("$.length()").value(1));
    }

    @ParameterizedTest
    @CsvSource({
            "2013-03-14, KM_5, 201",
            "2013-03-15, KM_5, 400",
            "2009-03-14, KM_10, 201",
            "2009-03-15, KM_10, 400",
            "2009-03-15, KM_5, 201",
            "2013-03-15, KM_10, 400"
    })
    void aplicaIdadeMinimaERestricaoDosDezKmPelaDataDaProva(String nascimento, String percurso, int statusEsperado)
            throws Exception {
        cadastrar("Pedro Lima", "pedro@email.com", nascimento, percurso)
                .andExpect(status().is(statusEsperado));
    }

    @Test
    void idadeMinimaTemPrecedenciaSobreARestricaoDosDezKm() throws Exception {
        cadastrar("Julia Lima", "julia@email.com", "2013-03-15", "KM_10")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A idade mínima para participar é 14 anos."));
    }

    @Test
    void dezKmParaMenorDe18DevolveAMensagemEspecifica() throws Exception {
        cadastrar("Pedro Lima", "pedro@email.com", "2009-03-15", "KM_10")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem")
                        .value("O percurso de 10 km é permitido somente a partir de 18 anos."));
    }

    @ParameterizedTest
    @CsvSource({
            "2013-03-14, JUVENIL",
            "2009-03-15, JUVENIL",
            "2009-03-14, ADULTO",
            "1987-03-15, ADULTO",
            "1987-03-14, MASTER",
            "1967-03-15, MASTER",
            "1967-03-14, SENIOR"
    })
    void categoriaCorretaNasBordas(String nascimento, String categoriaEsperada) throws Exception {
        cadastrar("Maria Souza", "maria@email.com", nascimento, "KM_5")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoria").value(categoriaEsperada));
    }

    @ParameterizedTest
    @MethodSource("cadastrosInvalidos")
    void cadastroInvalidoDevolve400ComMensagemClara(String corpo, String mensagemEsperada) throws Exception {
        mockMvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$.mensagem").value(mensagemEsperada));

        mockMvc.perform(get(ROTA)).andExpect(content().json("[]"));
    }

    static Stream<Arguments> cadastrosInvalidos() {
        String amanha = LocalDate.now().plusDays(1).toString();
        return Stream.of(
                Arguments.of("{}", "O e-mail é obrigatório."),
                Arguments.of(corpo("Ana Souza", "ana@email.com", "1990-05-20", null), "O percurso é obrigatório. Use KM_5 ou KM_10."),
                Arguments.of(corpo(null, "ana@email.com", "1990-05-20", "KM_5"), "O nome é obrigatório."),
                Arguments.of(corpo("Ab", "ana@email.com", "1990-05-20", "KM_5"), "O nome deve ter entre 3 e 100 caracteres."),
                Arguments.of(corpo("A".repeat(101), "ana@email.com", "1990-05-20", "KM_5"), "O nome deve ter entre 3 e 100 caracteres."),
                Arguments.of(corpo("Ana Souza", "ana-email.com", "1990-05-20", "KM_5"), "O e-mail informado é inválido."),
                Arguments.of(corpo("Ana Souza", "ana@email.com", null, "KM_5"), "A data de nascimento é obrigatória."),
                Arguments.of(corpo("Ana Souza", "ana@email.com", amanha, "KM_5"), "A data de nascimento deve ser anterior à data de hoje."),
                Arguments.of(corpo("Ana Souza", "ana@email.com", "2026-02-30", "KM_5"), "Valor inválido para o campo 'dataNascimento'. Informe uma data válida no formato aaaa-mm-dd."),
                Arguments.of(corpo("Ana Souza", "ana@email.com", "20/05/1990", "KM_5"), "Valor inválido para o campo 'dataNascimento'. Informe uma data válida no formato aaaa-mm-dd."),
                Arguments.of(corpo("Ana Souza", "ana@email.com", "1990-05-20", "KM_7"), "Valor inválido para o campo 'percurso'. Use KM_5 ou KM_10."),
                Arguments.of("{ \"nome\": \"Ana", "O corpo da requisição não é um JSON válido ou está ausente."));
    }

    @Test
    void listaVaziaDevolveArrayVazio() throws Exception {
        mockMvc.perform(get(ROTA))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void listaNaOrdemDeCadastroPreservandoNomesLongosEAcentos() throws Exception {
        String nomeLongo = "A".repeat(100);
        cadastrar("Bruno Alves", "bruno@email.com", "1990-05-20", "KM_5").andExpect(status().isCreated());
        cadastrar("José D'Ávila-Çalışkan", "jose@email.com", "1985-02-01", "KM_10").andExpect(status().isCreated());
        cadastrar(nomeLongo, "longo@email.com", "1970-12-31", "KM_5").andExpect(status().isCreated());

        mockMvc.perform(get(ROTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].email").value("bruno@email.com"))
                .andExpect(jsonPath("$[1].email").value("jose@email.com"))
                .andExpect(jsonPath("$[1].nome").value("José D'Ávila-Çalışkan"))
                .andExpect(jsonPath("$[2].nome").value(nomeLongo));
    }

    @Test
    void consultaPeloEmailMesmoComCaixaEEspacosDiferentes() throws Exception {
        cadastrar("Ana Souza", "ana.souza@email.com", "1990-05-20", "KM_10").andExpect(status().isCreated());

        mockMvc.perform(get(ROTA_POR_EMAIL, " Ana.Souza@Email.COM "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Souza"))
                .andExpect(jsonPath("$.idadeNaProva").value(36));
    }

    @Test
    void consultaDeInscricaoAusenteDevolve404() throws Exception {
        mockMvc.perform(get(ROTA_POR_EMAIL, "ninguem@email.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Não existe inscrição ativa para o e-mail informado."));
    }

    @Test
    void editaNomeNascimentoEPercursoMantendoEmailPosicaoETotal() throws Exception {
        cadastrar("Bruno Alves", "bruno@email.com", "1990-05-20", "KM_5").andExpect(status().isCreated());
        cadastrar("Ana Souza", "ana@email.com", "1990-05-20", "KM_5").andExpect(status().isCreated());
        String corpo = """
                {"nome": "  Bruno   Alves Filho ", "email": "outro@email.com",
                 "dataNascimento": "1960-03-14", "percurso": "KM_10"}""";

        mockMvc.perform(put(ROTA_POR_EMAIL, " BRUNO@email.com ").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Bruno Alves Filho"))
                .andExpect(jsonPath("$.email").value("bruno@email.com"))
                .andExpect(jsonPath("$.percurso").value("KM_10"))
                .andExpect(jsonPath("$.idadeNaProva").value(67))
                .andExpect(jsonPath("$.categoria").value("SENIOR"));

        mockMvc.perform(get(ROTA))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].email").value("bruno@email.com"))
                .andExpect(jsonPath("$[1].email").value("ana@email.com"));
        mockMvc.perform(get(ROTA + "/resumo"))
                .andExpect(content().json("{\"total\": 2, \"km5\": 1, \"km10\": 1}"));
    }

    @Test
    void edicaoRecusadaPreservaDadosETotaisAnteriores() throws Exception {
        cadastrar("Ana Souza", "ana@email.com", "1990-05-20", "KM_5").andExpect(status().isCreated());

        editar("ana@email.com", "Ana Nova", "2009-03-15", "KM_10")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem")
                        .value("O percurso de 10 km é permitido somente a partir de 18 anos."));

        mockMvc.perform(get(ROTA_POR_EMAIL, "ana@email.com"))
                .andExpect(jsonPath("$.nome").value("Ana Souza"))
                .andExpect(jsonPath("$.dataNascimento").value("1990-05-20"))
                .andExpect(jsonPath("$.percurso").value("KM_5"));
        mockMvc.perform(get(ROTA + "/resumo"))
                .andExpect(content().json("{\"total\": 1, \"km5\": 1, \"km10\": 0}"));
    }

    @Test
    void edicaoComDataInexistentePreservaOsDados() throws Exception {
        cadastrar("Ana Souza", "ana@email.com", "1990-05-20", "KM_5").andExpect(status().isCreated());

        editar("ana@email.com", "Ana Nova", "2026-02-30", "KM_5")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem")
                        .value("Valor inválido para o campo 'dataNascimento'. Informe uma data válida no formato aaaa-mm-dd."));

        mockMvc.perform(get(ROTA_POR_EMAIL, "ana@email.com")).andExpect(jsonPath("$.nome").value("Ana Souza"));
    }

    @Test
    void edicaoDeInscricaoAusenteDevolve404() throws Exception {
        editar("ninguem@email.com", "Ana Souza", "1990-05-20", "KM_5")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Não existe inscrição ativa para o e-mail informado."));
    }

    @Test
    void cancelaDevolve204SemCorpoELiberaORecadastroNoFimDaLista() throws Exception {
        cadastrar("Ana Souza", "ana@email.com", "1990-05-20", "KM_5").andExpect(status().isCreated());
        cadastrar("Bruno Alves", "bruno@email.com", "1990-05-20", "KM_5").andExpect(status().isCreated());

        mockMvc.perform(delete(ROTA_POR_EMAIL, " ANA@email.com "))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        mockMvc.perform(get(ROTA_POR_EMAIL, "ana@email.com")).andExpect(status().isNotFound());

        cadastrar("Ana Nova", "ana@email.com", "1991-01-01", "KM_10").andExpect(status().isCreated());
        mockMvc.perform(get(ROTA))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].email").value("bruno@email.com"))
                .andExpect(jsonPath("$[1].email").value("ana@email.com"))
                .andExpect(jsonPath("$[1].nome").value("Ana Nova"));
    }

    @Test
    void cancelarAusenteDevolve404SemAlterarOsDemais() throws Exception {
        cadastrar("Ana Souza", "ana@email.com", "1990-05-20", "KM_5").andExpect(status().isCreated());

        mockMvc.perform(delete(ROTA_POR_EMAIL, "ninguem@email.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Não existe inscrição ativa para o e-mail informado."));
        mockMvc.perform(get(ROTA)).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void resumoVazioTemTodosOsTotaisZerados() throws Exception {
        mockMvc.perform(get(ROTA + "/resumo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.km5").value(0))
                .andExpect(jsonPath("$.km10").value(0));
    }

    @Test
    void resumoAcompanhaCadastroEdicaoECancelamento() throws Exception {
        cadastrar("Ana Souza", "ana@email.com", "1990-05-20", "KM_5").andExpect(status().isCreated());
        cadastrar("Bruno Alves", "bruno@email.com", "1990-05-20", "KM_5").andExpect(status().isCreated());
        cadastrar("Carla Dias", "carla@email.com", "1990-05-20", "KM_10").andExpect(status().isCreated());
        mockMvc.perform(get(ROTA + "/resumo")).andExpect(content().json("{\"total\": 3, \"km5\": 2, \"km10\": 1}"));

        editar("ana@email.com", "Ana Souza", "1990-05-20", "KM_10").andExpect(status().isOk());
        mockMvc.perform(get(ROTA + "/resumo")).andExpect(content().json("{\"total\": 3, \"km5\": 1, \"km10\": 2}"));

        mockMvc.perform(delete(ROTA_POR_EMAIL, "carla@email.com")).andExpect(status().isNoContent());
        mockMvc.perform(get(ROTA + "/resumo")).andExpect(content().json("{\"total\": 2, \"km5\": 1, \"km10\": 1}"));
    }

    @Test
    void metodoNaoPermitidoUsaOFormatoDoContrato() throws Exception {
        mockMvc.perform(patch(ROTA_POR_EMAIL, "ana@email.com"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.mensagem").value("Método HTTP não permitido para esta rota."));
    }

    @Test
    void rotaInexistenteUsaOFormatoDoContrato() throws Exception {
        mockMvc.perform(get("/api/outra-coisa"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Recurso não encontrado."));
    }

    private ResultActions cadastrar(String nome, String email, String nascimento, String percurso) throws Exception {
        return mockMvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON)
                .content(corpo(nome, email, nascimento, percurso)));
    }

    private ResultActions editar(String email, String nome, String nascimento, String percurso) throws Exception {
        String corpo = """
                {"nome": "%s", "dataNascimento": "%s", "percurso": "%s"}""".formatted(nome, nascimento, percurso);
        return mockMvc.perform(put(ROTA_POR_EMAIL, email).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private static String corpo(String nome, String email, String nascimento, String percurso) {
        return "{" + campo("nome", nome) + "," + campo("email", email) + ","
                + campo("dataNascimento", nascimento) + "," + campo("percurso", percurso) + "}";
    }

    private static String campo(String nome, String valor) {
        return "\"" + nome + "\":" + (valor == null ? "null" : "\"" + valor + "\"");
    }
}
