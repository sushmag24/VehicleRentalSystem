package com.vehiclerental.filter;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class SpaWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Check if the request is a GET and does not target API/Eureka or static files
        if (exchange.getRequest().getMethod().name().equalsIgnoreCase("GET")
                && !path.startsWith("/api")
                && !path.startsWith("/eureka")
                && !path.startsWith("/actuator")
                && !path.contains(".")) {
            
            // Forward internally to index.html
            return chain.filter(exchange.mutate()
                    .request(exchange.getRequest().mutate().path("/index.html").build())
                    .build());
        }
        return chain.filter(exchange);
    }
}
