package com.aptidao.backend.config;

import com.aptidao.backend.service.RegulamentoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CorridaConfigTest {

    @Autowired
    private CorridaProperties properties;

    @Autowired
    private RegulamentoService regulamento;

    @Test
    void leDataDaProvaDoApplicationProperties() {
        assertThat(properties.dataProva()).isEqualTo(LocalDate.of(2027, 3, 14));
    }

    @Test
    void regulamentoUsaADataConfigurada() {
        assertThat(regulamento.calcularIdadeNaProva(LocalDate.of(2009, 3, 14))).isEqualTo(18);
        assertThat(regulamento.calcularIdadeNaProva(LocalDate.of(2009, 3, 15))).isEqualTo(17);
    }
}
