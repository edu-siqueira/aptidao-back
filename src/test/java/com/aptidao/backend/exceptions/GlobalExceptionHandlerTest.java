package com.aptidao.backend.exceptions;

import com.aptidao.backend.dto.InscricaoPostDTO;
import com.aptidao.backend.dto.InscricaoDTO;
import com.aptidao.backend.enums.Categoria;
import com.aptidao.backend.enums.Percurso;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ControladorFalso())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void regraDeNegocioViraBadRequestComAMensagemDoRegulamento() throws Exception {
        mockMvc.perform(get("/regra"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A idade mínima para participar é 14 anos."));
    }

    @Test
    void inscricaoAusenteViraNotFound() throws Exception {
        mockMvc.perform(get("/ausente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Não existe inscrição ativa para o e-mail informado."));
    }

    @Test
    void emailRepetidoViraConflict() throws Exception {
        mockMvc.perform(get("/duplicada"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("O e-mail informado já possui uma inscrição ativa."));
    }

    @Test
    void erroInesperadoNaoExpoeDetalhesInternos() throws Exception {
        mockMvc.perform(get("/inesperado"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.mensagem").value("Ocorreu um erro interno. Tente novamente mais tarde."))
                .andExpect(content().string(not(containsString("detalhe interno"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    @Test
    void jsonQuebradoViraBadRequest() throws Exception {
        mockMvc.perform(post("/corpo").contentType(MediaType.APPLICATION_JSON).content("{ \"nome\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O corpo da requisição não é um JSON válido ou está ausente."));
    }

    @Test
    void corpoAusenteViraBadRequest() throws Exception {
        mockMvc.perform(post("/corpo").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O corpo da requisição não é um JSON válido ou está ausente."));
    }

    @Test
    void dataInexistenteViraBadRequestIndicandoOCampo() throws Exception {
        mockMvc.perform(post("/corpo").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dataNascimento\": \"2026-02-30\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem")
                        .value("Valor inválido para o campo 'dataNascimento'. Informe uma data válida no formato aaaa-mm-dd."));
    }

    @Test
    void percursoDesconhecidoViraBadRequestIndicandoOCampo() throws Exception {
        mockMvc.perform(post("/corpo").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"percurso\": \"KM_7\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Valor inválido para o campo 'percurso'. Use KM_5 ou KM_10."));
    }

    @Test
    void metodoNaoPermitidoUsaOFormatoDoContrato() throws Exception {
        mockMvc.perform(get("/corpo"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.mensagem").value("Método HTTP não permitido para esta rota."));
    }

    @Test
    void tipoDeConteudoNaoSuportadoUsaOFormatoDoContrato() throws Exception {
        mockMvc.perform(post("/corpo").contentType(MediaType.TEXT_PLAIN).content("abc"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.mensagem")
                        .value("Tipo de conteúdo não suportado. Envie o corpo como application/json."));
    }

    @Test
    void respostaDaInscricaoTemOsSeisCamposEDataSemDeslocamento() throws Exception {
        mockMvc.perform(get("/resposta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$.nome").value("Ana Souza"))
                .andExpect(jsonPath("$.dataNascimento").value("1990-05-20"))
                .andExpect(jsonPath("$.percurso").value("KM_10"))
                .andExpect(jsonPath("$.idadeNaProva").value(36))
                .andExpect(jsonPath("$.categoria").value("ADULTO"));
    }

    @RestController
    static class ControladorFalso {

        @GetMapping("/regra")
        void regra() {
            throw new RegraNegocioException("A idade mínima para participar é 14 anos.");
        }

        @GetMapping("/ausente")
        void ausente() {
            throw new InscricaoNaoEncontradaException();
        }

        @GetMapping("/duplicada")
        void duplicada() {
            throw new InscricaoDuplicadaException();
        }

        @GetMapping("/inesperado")
        void inesperado() {
            throw new IllegalStateException("detalhe interno");
        }

        @PostMapping("/corpo")
        void corpo(@RequestBody InscricaoPostDTO request) {
        }

        @GetMapping("/resposta")
        InscricaoDTO resposta() {
            return new InscricaoDTO(
                    "Ana Souza", "ana.souza@email.com", LocalDate.of(1990, 5, 20), Percurso.KM_10, 36, Categoria.ADULTO);
        }
    }
}
