package com.aptidao.backend.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;

@Validated
@ConfigurationProperties(prefix = "corrida")
public record CorridaProperties(@NotNull LocalDate dataProva) {
}
