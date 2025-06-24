package com.davidcamelo.api.gateway.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.davidcamelo.api.gateway.config.JWTProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
public class AuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthenticationGatewayFilterFactory.Config> {
    private final JWTProperties jwtProperties;

    private static final List<String> EXCLUDED_PATHS = List.of(
            "/auth", "/actuator", "/info", "/swagger-ui", "/v3/api-docs"
    );

    private static final List<String> EXCLUDED_ORIGINS = List.of(
            "http://localhost:6006", "http://localhost:8080",
            "http://localhost:4173", "http://localhost:4174", "http://localhost:4175", "http://localhost:4176",
            "http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5176"
    );

    public AuthenticationGatewayFilterFactory(JWTProperties jwtProperties) {
        super(Config.class);
        this.jwtProperties = jwtProperties;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            var request = exchange.getRequest();
            if (EXCLUDED_PATHS.stream().anyMatch(path -> request.getURI().getPath().startsWith(path))
                || EXCLUDED_ORIGINS.stream().anyMatch(origin -> request.getHeaders().getOrigin() != null && request.getHeaders().getOrigin().equals(origin))
            ) {
                return chain.filter(exchange);
            }
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange);
            }
            var authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange);
            }
            var token = authHeader.substring(7);
            try {
                var decodedJWT = JWT.require(Algorithm.HMAC256(jwtProperties.secret()))
                        .build()
                        .verify(token);
                var subject = decodedJWT.getSubject();
                var roles = decodedJWT.getClaim("roles").asList(String.class);
                var mutatedRequest = request.mutate()
                        .header("X-User-Id", subject)
                        .header("X-User-Roles", roles.toString())
                        .build();
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            } catch (Exception e) {
                return onError(exchange);
            }
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange) {
        log.error("Unauthorized request: {}", exchange.getRequest().getURI());
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
        // Configuration properties can be added here if needed
    }
}
