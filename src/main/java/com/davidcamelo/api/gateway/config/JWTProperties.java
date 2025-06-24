package com.davidcamelo.api.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JWTProperties(
        String secret
) { }
