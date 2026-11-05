package com.aptidao.backend.service;

import com.aptidao.backend.dto.InscricaoPutDTO;
import com.aptidao.backend.dto.InscricaoPostDTO;
import com.aptidao.backend.dto.InscricaoDTO;
import com.aptidao.backend.dto.InscricaoResumoDTO;
import com.aptidao.backend.enums.Percurso;
import com.aptidao.backend.exceptions.InscricaoDuplicadaException;
import com.aptidao.backend.exceptions.InscricaoNaoEncontradaException;
import com.aptidao.backend.model.Inscricao;
import com.aptidao.backend.repository.InscricaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InscricaoService {

    private final InscricaoRepository repository;
    private final RegulamentoService regulamento;
    private final ValidadorInscricao validador;

    @Transactional
    public InscricaoDTO cadastrar(InscricaoPostDTO request) {
        String email = validador.normalizarEmail(request.email());
        String nome = validador.normalizarNome(request.nome());
        validador.validarEmail(email);
        validarDados(nome, request.dataNascimento(), request.percurso());

        if (repository.existsByEmail(email)) {
            throw new InscricaoDuplicadaException();
        }
        try {
            Inscricao inscricao = new Inscricao(nome, email, request.dataNascimento(), request.percurso());
            return converterParaDTO(repository.saveAndFlush(inscricao));
        } catch (DataIntegrityViolationException ex) {
            // duas requisições simultâneas com o mesmo e-mail: a constraint do banco desempata
            throw new InscricaoDuplicadaException();
        }
    }

    @Transactional(readOnly = true)
    public List<InscricaoDTO> listar() {
        return repository.findAllByOrderByIdAsc().stream().map(this::converterParaDTO).toList();
    }

    @Transactional(readOnly = true)
    public InscricaoDTO consultar(String email) {
        return converterParaDTO(buscar(email));
    }

    @Transactional
    public InscricaoDTO editar(String email, InscricaoPutDTO request) {
        Inscricao inscricao = buscar(email);
        String nome = validador.normalizarNome(request.nome());
        validarDados(nome, request.dataNascimento(), request.percurso());

        inscricao.setNome(nome);
        inscricao.setDataNascimento(request.dataNascimento());
        inscricao.setPercurso(request.percurso());
        return converterParaDTO(inscricao);
    }

    @Transactional
    public void cancelar(String email) {
        if (repository.deleteByEmail(validador.normalizarEmail(email)) == 0) {
            throw new InscricaoNaoEncontradaException();
        }
    }

    @Transactional(readOnly = true)
    public InscricaoResumoDTO resumo() {
        List<Inscricao> inscricoes = repository.findAllByOrderByIdAsc();
        long km5 = contarPorPercurso(inscricoes, Percurso.KM_5);
        long km10 = contarPorPercurso(inscricoes, Percurso.KM_10);
        return new InscricaoResumoDTO(inscricoes.size(), km5, km10);
    }

    private void validarDados(String nomeNormalizado, LocalDate dataNascimento, Percurso percurso) {
        validador.validarNome(nomeNormalizado);
        validador.validarDataNascimento(dataNascimento);
        validador.validarPercurso(percurso);
        regulamento.validarParticipacao(regulamento.calcularIdadeNaProva(dataNascimento), percurso);
    }

    private Inscricao buscar(String email) {
        return repository.findByEmail(validador.normalizarEmail(email))
                .orElseThrow(InscricaoNaoEncontradaException::new);
    }

    private long contarPorPercurso(List<Inscricao> inscricoes, Percurso percurso) {
        return inscricoes.stream().filter(inscricao -> inscricao.getPercurso() == percurso).count();
    }

    private InscricaoDTO converterParaDTO(Inscricao inscricao) {
        int idade = regulamento.calcularIdadeNaProva(inscricao.getDataNascimento());
        return new InscricaoDTO(
                inscricao.getNome(),
                inscricao.getEmail(),
                inscricao.getDataNascimento(),
                inscricao.getPercurso(),
                idade,
                regulamento.definirCategoria(idade));
    }
}
