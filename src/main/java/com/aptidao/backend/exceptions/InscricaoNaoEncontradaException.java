package com.aptidao.backend.exceptions;

public class InscricaoNaoEncontradaException extends RuntimeException {

    public InscricaoNaoEncontradaException() {
        super("Não existe inscrição ativa para o e-mail informado.");
    }
}
