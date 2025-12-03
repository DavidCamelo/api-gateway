package com.davidcamelo.api.gateway.config;

import lombok.RequiredArgsConstructor;
import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.HashSet;

import static org.springdoc.core.utils.Constants.DEFAULT_API_DOCS_URL;

@Configuration
@RequiredArgsConstructor
public class SwaggerConfig {
    private final RouteDefinitionLocator routeDefinitionLocator;
    private final SwaggerUiConfigProperties swaggerUiConfigProperties;

    @Scheduled(fixedDelay = 60_000)
    public void updateSwaggerDocs() {
        var definitions = routeDefinitionLocator.getRouteDefinitions().collectList().block();
        var urls = new HashSet<AbstractSwaggerUiConfigProperties.SwaggerUrl>();
        urls.add(new AbstractSwaggerUiConfigProperties.SwaggerUrl("", DEFAULT_API_DOCS_URL, null));
        assert definitions != null;
        definitions.stream()
                .filter(routeDefinition -> (
                        routeDefinition.getId() != null
                        && routeDefinition.getId().startsWith("ReactiveCompositeDiscoveryClient")
                        && !routeDefinition.getId().endsWith("EUREKA-SERVER")
                        && !routeDefinition.getId().endsWith("API-GATEWAY")
                        && !routeDefinition.getId().endsWith("CONFIG-SERVER"))
                )
                .forEach(routeDefinition -> {
                    var name = routeDefinition.getId().substring(routeDefinition.getId().lastIndexOf("_") + 1).toLowerCase();
                    var swaggerUrl = new AbstractSwaggerUiConfigProperties.SwaggerUrl(name, DEFAULT_API_DOCS_URL + "/" + name, name + "-service");
                    urls.add(swaggerUrl);
                });
        swaggerUiConfigProperties.setUrls(urls);
    }
}