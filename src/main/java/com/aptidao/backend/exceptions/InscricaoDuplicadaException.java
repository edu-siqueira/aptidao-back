package com.aptidao.backend.exceptions;

public class InscricaoDuplicadaException extends RuntimeException {

    public InscricaoDuplicadaException() {
        super("O e-mail informado já possui uma inscrição ativa.");
    }
}
