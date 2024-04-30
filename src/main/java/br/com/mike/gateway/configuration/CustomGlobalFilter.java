package br.com.mike.gateway.configuration;

import br.com.mike.gateway.recordys.ApiPorta;
import br.com.mike.gateway.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CustomGlobalFilter implements GlobalFilter {

    @Autowired
    private RedisService repository;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        System.out.println("Iniciando redirecionamento...");
        if (!exchange.getRequest().getPath().value().contains("docker")) {
            List<String> endpoints = Arrays.stream(exchange.getRequest()
                            .getPath().value().split("/"))
                    .filter(x -> !x.isBlank() && !x.contains("api")).toList();
            List<ApiPorta> portas = repository.findAllByEndpoint(endpoints.get(0));
            portas.removeIf(x -> x.getQuantidadeRequisicao() > 100L);
            ApiPorta porta = portas.get(0);
            porta.setQuantidadeRequisicao(porta.getQuantidadeRequisicao() + 1);
            Thread thread = new Thread((Runnable) () -> {
                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<ApiPorta> entity = new HttpEntity<>(porta,headers);
                ApiPorta value = restTemplate.exchange(
                        "http://localhost:8080/api/docker/save",
                        HttpMethod.POST,
                        entity,
                        new ParameterizedTypeReference<ApiPorta>() {}
                ).getBody();
                if (value != null) {
                    repository.save(value);
                }
            });
            thread.start();
            ServerWebExchange mutated = exchange.mutate().request(builder -> {
                try {
                    builder.uri(new URI("http://localhost:" + porta.getPorta() + exchange.getRequest().getPath().value()));
                    for (Map.Entry<String, String> value : exchange.getRequest().getHeaders().toSingleValueMap().entrySet()) {
                        builder.header(value.getKey(), value.getValue());
                    }
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }).build();
            return chain.filter(mutated).then(Mono.fromRunnable(() -> {
                porta.setQuantidadeRequisicao(porta.getQuantidadeRequisicao() - 1);
                Thread thread1 = new Thread((Runnable) () -> {
                    RestTemplate restTemplate = new RestTemplate();
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<ApiPorta> entity = new HttpEntity<>(porta,headers);
                    ApiPorta value = restTemplate.exchange(
                            "http://localhost:8080/api/docker/save",
                            HttpMethod.POST,
                            entity,
                            new ParameterizedTypeReference<ApiPorta>() {}
                    ).getBody();
                    if (value != null) {
                        repository.save(value);
                    }
                });
                thread1.start();
                System.out.println("Redirecionamento concluído.");
            }));
        }
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            System.out.println("Redirecionamento concluído.");
        }));
    }
}
