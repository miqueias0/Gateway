package br.com.mike.gateway.configuration;

import br.com.mike.gateway.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

//@Configuration
@Component
public class RouterConfiguration {

    @Autowired
    public RedisService config;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("desvio_rota_docker", r -> r.path("/api/docker/*")
//                        .customize(a -> a.predicate(customRoutePredicateFactory.apply(new CustomRoutePredicateFactory.Config())))
//                        .filters(f -> f.filter(new CustomRouteChangingFilter().apply(new CustomRouteChangingFilter.Config(config))))
                        .uri("http://localhost:8080"))
                .route("desvio_rota", r -> r.path("/api/**")
                        .filters(f -> f.filter(new CustomRouteChangingFilter().apply(new CustomRouteChangingFilter.Config(config))))
                        .uri("http://localhost:8081"))
                .build();
    }


//    @Autowired
//    private RouterDefinitionLocatorDinamic routerDefinitionLocatorDinamic;

//    @Bean
//    public RouteDefinitionLocator customRouteLocator(RouteLocatorBuilder builder) {
//        return routerDefinitionLocatorDinamic;
//    }
}
