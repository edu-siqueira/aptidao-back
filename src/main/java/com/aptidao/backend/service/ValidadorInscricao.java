package com.aptidao.backend.service;

import com.aptidao.backend.enums.Percurso;
import com.aptidao.backend.exceptions.RegraNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class ValidadorInscricao {

    private static final int NOME_TAMANHO_MINIMO = 3;
    private static final int NOME_TAMANHO_MAXIMO = 100;
    private static final int EMAIL_TAMANHO_MAXIMO = 254;
    private static final Pattern ESPACOS = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern EMAIL_FORMATO =
            Pattern.compile("^[^\\s@]+@([^\\s@.]+\\.)+[^\\s@.]+$", Pattern.UNICODE_CHARACTER_CLASS);

    private final Clock clock;

    public String normalizarNome(String nome) {
        return nome == null ? null : ESPACOS.matcher(nome.strip()).replaceAll(" ");
    }

    public String normalizarEmail(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    public void validarNome(String nomeNormalizado) {
        if (nomeNormalizado == null || nomeNormalizado.isEmpty()) {
            throw new RegraNegocioException("O nome é obrigatório.");
        }
        if (nomeNormalizado.length() < NOME_TAMANHO_MINIMO || nomeNormalizado.length() > NOME_TAMANHO_MAXIMO) {
            throw new RegraNegocioException(
                    "O nome deve ter entre " + NOME_TAMANHO_MINIMO + " e " + NOME_TAMANHO_MAXIMO + " caracteres.");
        }
    }

    public void validarEmail(String emailNormalizado) {
        if (emailNormalizado == null || emailNormalizado.isEmpty()) {
            throw new RegraNegocioException("O e-mail é obrigatório.");
        }
        if (emailNormalizado.length() > EMAIL_TAMANHO_MAXIMO || !EMAIL_FORMATO.matcher(emailNormalizado).matches()) {
            throw new RegraNegocioException("O e-mail informado é inválido.");
        }
    }

    public void validarDataNascimento(LocalDate dataNascimento) {
        if (dataNascimento == null) {
            throw new RegraNegocioException("A data de nascimento é obrigatória.");
        }
        if (!dataNascimento.isBefore(LocalDate.now(clock))) {
            throw new RegraNegocioException("A data de nascimento deve ser anterior à data de hoje.");
        }
    }

    public void validarPercurso(Percurso percurso) {
        if (percurso == null) {
            throw new RegraNegocioException("O percurso é obrigatório. Use KM_5 ou KM_10.");
        }
    }
}
