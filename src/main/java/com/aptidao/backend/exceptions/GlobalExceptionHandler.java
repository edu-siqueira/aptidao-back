package com.aptidao.backend.exceptions;

import com.aptidao.backend.dto.ErroDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.databind.DatabindException;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Map<String, String> dicasPorCampo = Map.of(
            "dataNascimento", "Informe uma data válida no formato aaaa-mm-dd.",
            "percurso", "Use KM_5 ou KM_10.");

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroDTO> tratarRegraNegocio(RegraNegocioException ex) {
        return resposta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InscricaoNaoEncontradaException.class)
    public ResponseEntity<ErroDTO> tratarNaoEncontrada(InscricaoNaoEncontradaException ex) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InscricaoDuplicadaException.class)
    public ResponseEntity<ErroDTO> tratarDuplicada(InscricaoDuplicadaException ex) {
        return resposta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroDTO> tratarInesperado(Exception ex) {
        log.error("Erro inesperado", ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, mensagemPadrao(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return new ResponseEntity<>(new ErroDTO(mensagemCorpoInvalido(ex)), HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        return new ResponseEntity<>(new ErroDTO(mensagemPadrao(statusCode)), headers, statusCode);
    }

    private ResponseEntity<ErroDTO> resposta(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(new ErroDTO(mensagem));
    }

    private String mensagemCorpoInvalido(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof DatabindException erro && !erro.getPath().isEmpty()) {
            String campo = erro.getPath().get(0).getPropertyName();
            String dica = dicasPorCampo.getOrDefault(campo, "Verifique o valor informado.");
            return "Valor inválido para o campo '" + campo + "'. " + dica;
        }
        return "O corpo da requisição não é um JSON válido ou está ausente.";
    }

    private String mensagemPadrao(HttpStatusCode status) {
        return switch (status.value()) {
            case 404 -> "Recurso não encontrado.";
            case 405 -> "Método HTTP não permitido para esta rota.";
            case 415 -> "Tipo de conteúdo não suportado. Envie o corpo como application/json.";
            case 500 -> "Ocorreu um erro interno. Tente novamente mais tarde.";
            default -> "Requisição inválida.";
        };
    }
}
