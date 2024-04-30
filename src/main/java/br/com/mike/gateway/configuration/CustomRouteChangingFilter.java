package br.com.mike.gateway.configuration;

import br.com.mike.gateway.recordys.ApiPorta;
import br.com.mike.gateway.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.*;

@Component
public class CustomRouteChangingFilter extends AbstractGatewayFilterFactory<CustomRouteChangingFilter.Config> {

    public CustomRouteChangingFilter() {
        super(Config.class);
    }

    private Map<String, ApiPorta> portas = new HashMap<>();
    private RedisService repository;

    @Override
    public GatewayFilter apply(Config config) {
        repository = config.repository;
        return (exchange, chain) -> {
            // Lógica para determinar a nova rota
            redirecionarExchange(exchange);
            // Continua com a cadeia de filtros
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                ApiPorta porta = portas.get(exchange.getRequest().getId());
                if(porta != null) {
                    porta.setQuantidadeRequisicao(porta.getQuantidadeRequisicao() - 1);
                    salvarDadosPorta(porta);
                    portas.remove(exchange.getRequest().getId());
                }
//                System.out.println("Redirecionamento concluído." + exchange.getRequest().getId());
            }));
        };
    }

    private void redirecionarExchange(ServerWebExchange exchange){
        if (!exchange.getRequest().getPath().value().contains("docker")) {
            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            assert route != null;
            ApiPorta porta = obterPorta(exchange);
            portas.put(exchange.getRequest().getId(), porta);
            route = Route.async()
                    .id(porta.getEndpoint() + ":" + porta.getPorta())
                    .asyncPredicate(route.getPredicate())
                    .uri(determineNewRoute(exchange))
                    .build();
            exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);
        }
    }

    private String determineNewRoute(ServerWebExchange exchange) {
        ApiPorta porta = portas.get(exchange.getRequest().getId());
        porta.setQuantidadeRequisicao(porta.getQuantidadeRequisicao() + 1);
        salvarDadosPorta(porta);
        return "http://localhost:" + porta.getPorta() + exchange.getRequest().getPath().value();
    }

    private ApiPorta obterPorta(ServerWebExchange exchange){
        List<String> endpoints = Arrays.stream(exchange.getRequest()
                        .getPath().value().split("/"))
                .filter(x -> !x.isBlank() && !x.contains("api")).toList();
        List<ApiPorta> portas = repository.findAllByEndpoint(endpoints.get(0));
        if(portas == null || portas.isEmpty()){
            RestTemplate restTemplate = new RestTemplate();
            portas = new ArrayList<>(Arrays.asList(restTemplate.exchange(
                    "http://localhost:8080/api/docker/subirComNome?container=" + endpoints.get(0),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ApiPorta>() {
                    }
            ).getBody()));
        }
        if(portas.size() > 1){
            portas.removeIf(x -> x.getQuantidadeRequisicao() > 100L);
        }
        return portas.get(0);
    }

    public void salvarDadosPorta(ApiPorta porta){
        new Thread((Runnable) () -> {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            porta.setDataUltimaRequisicao(new Date());
            HttpEntity<ApiPorta> entity = new HttpEntity<>(porta, headers);
            ApiPorta value = restTemplate.exchange(
                    "http://localhost:8080/api/docker/save",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<ApiPorta>() {
                    }
            ).getBody();
            if (value != null) {
                repository.save(value);
            }
        }).start();
    }

    public static class Config {
        public RedisService repository;

        @Autowired
        public Config(RedisService repository) {
            this.repository = repository;
        }
    }
}
