package com.aptidao.backend.controller;

import com.aptidao.backend.dto.InscricaoDTO;
import com.aptidao.backend.dto.InscricaoPostDTO;
import com.aptidao.backend.dto.InscricaoPutDTO;
import com.aptidao.backend.dto.InscricaoResumoDTO;
import com.aptidao.backend.service.InscricaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inscricoes")
@RequiredArgsConstructor
public class InscricaoController {

    private final InscricaoService service;

    @PostMapping
    public ResponseEntity<InscricaoDTO> cadastrar(@RequestBody InscricaoPostDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.cadastrar(request));
    }

    @GetMapping
    public ResponseEntity<List<InscricaoDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/resumo")
    public ResponseEntity<InscricaoResumoDTO> resumo() {
        return ResponseEntity.ok(service.resumo());
    }

    @GetMapping("/{email}")
    public ResponseEntity<InscricaoDTO> consultar(@PathVariable String email) {
        return ResponseEntity.ok(service.consultar(email));
    }

    @PutMapping("/{email}")
    public ResponseEntity<InscricaoDTO> editar(@PathVariable String email, @RequestBody InscricaoPutDTO request) {
        return ResponseEntity.ok(service.editar(email, request));
    }

    @DeleteMapping("/{email}")
    public ResponseEntity<Void> cancelar(@PathVariable String email) {
        service.cancelar(email);
        return ResponseEntity.noContent().build();
    }
}
