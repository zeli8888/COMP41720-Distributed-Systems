package zeli8888.lab4.apigateway.routes;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class Routes {

    @Bean
    public RouterFunction<ServerResponse> fallbackRoute() {
        return route(GET("/fallbackRoute"), request ->
                ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Mono.just("Service Unavailable, please try again later"), String.class)
        );
    }
}