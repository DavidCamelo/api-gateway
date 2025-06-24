package com.davidcamelo.api.gateway.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthenticationGatewayFilterFactory.Config> {

    @Value("${jwt.secret}")
    private String secret;

    public AuthenticationGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            var request = exchange.getRequest();
            if (request.getURI().getPath().startsWith("/auth")
                    || request.getURI().getPath().startsWith("/actuator")
                    || request.getURI().getPath().contains("/swagger-ui")
                    || request.getURI().getPath().contains("/v3/api-docs")) {
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
                var decodedJWT = JWT.require(Algorithm.HMAC256(secret))
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
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
        // Configuration properties can be added here if needed
    }
}
