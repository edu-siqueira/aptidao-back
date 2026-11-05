package com.aptidao.backend.config;

import com.aptidao.backend.service.RegulamentoService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CorridaProperties.class)
public class CorridaConfig {

    @Bean
    public RegulamentoService regulamentoService(CorridaProperties properties) {
        return new RegulamentoService(properties.dataProva());
    }
}
