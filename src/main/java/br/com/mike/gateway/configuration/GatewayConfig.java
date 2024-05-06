package br.com.mike.gateway.configuration;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.filter.reactive.HiddenHttpMethodFilter;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayConfig {

//    @Bean
//    public GlobalFilter customGlobalFilter() {
//        return (exchange, chain) -> {
//            // Logica do filtro
//            System.out.println("Request to: " + exchange.getRequest().getURI());
//            return chain.filter(exchange);
//        };
//    }

    @Bean
    public WebFilter corsFilter() {
        return (ServerWebExchange ctx, WebFilterChain chain) -> {
            if (CorsUtils.isCorsRequest(ctx.getRequest())) {
                if(ctx.getRequest().getMethod() == HttpMethod.OPTIONS) {
                    ctx.getResponse().getHeaders().add("Access-Control-Allow-Origin", "*");
                    ctx.getResponse().getHeaders().add("Access-Control-Allow-Methods", "GET, PUT, POST, DELETE, OPTIONS");
                    ctx.getResponse().getHeaders().add("Access-Control-Max-Age", "3600");
                    ctx.getResponse().getHeaders().add("Access-Control-Allow-Headers", "authorization, content-type");
                }
                if (ctx.getRequest().getMethod() == HttpMethod.OPTIONS) {
                    ctx.getResponse().setStatusCode(HttpStatus.OK);
                    return Mono.empty();
                }
            }
            return chain.filter(ctx);
        };
    }

    // Você pode adicionar outros filtros aqui, se necessário

//    @Bean
//    public HiddenHttpMethodFilter hiddenHttpMethodFilter() {
//        return new HiddenHttpMethodFilter();
//    }

//    @Bean
//    public GlobalFilter customGlobalFilter1() {
//        return new CustomGlobalFilter();
//    }
//
//    @Bean
//    public GlobalFilter postGlobalFilter() {
//        return new PostGlobalFilter();
//    }
}
