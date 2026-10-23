package com.henrique.radiologyexams.config;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "orthanc")
public record OrthancProperties (
        @NotBlank String baseUrl,
        @NotBlank String username,
        @NotBlank String password,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout
){
}
