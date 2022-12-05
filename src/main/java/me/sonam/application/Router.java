package me.sonam.application;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import me.sonam.application.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;

/**
 * Set AccountService methods route for checking active and to actiate acccount
 */
@Configuration
@OpenAPIDefinition(info = @Info(title = "Swagger Demo", version = "1.0", description = "Documentation APIs v1.0"))

public class Router {
    private static final Logger LOG = LoggerFactory.getLogger(Router.class);

    @Bean
    @RouterOperations(
            {
                    @RouterOperation(path = "/applications"
                    , produces = {
                        MediaType.APPLICATION_JSON_VALUE}, method= RequestMethod.GET,
                         operation = @Operation(operationId="activeUserId", responses = {
                            @ApiResponse(responseCode = "200", description = "successful operation"),
                                 @ApiResponse(responseCode = "400", description = "invalid user id")}
                    ))
            }
    )
    public RouterFunction<ServerResponse> route(Handler handler) {
        LOG.info("building router function");
        return RouterFunctions.route(POST("/applications").and(accept(MediaType.APPLICATION_JSON)),
                handler::createApplication)
                .andRoute(PUT("/applications")
                        .and(accept(MediaType.APPLICATION_JSON)), handler::updateApplication)
                .andRoute(DELETE("/applications/{applicationId}")
                        .and(accept(MediaType.APPLICATION_JSON)), handler::deleteApplication)
                .andRoute(GET("/applications")
                        .and(accept(MediaType.APPLICATION_JSON)), handler::getApplications)
                .andRoute(GET("/applications/{organizationId}")
                        .and(accept(MediaType.APPLICATION_JSON)), handler::getOrganizationApplications)
                .andRoute(PUT("/applications/users")
                        .and(accept(MediaType.APPLICATION_JSON)), handler::updateApplicationUsers)
                .andRoute(GET("/applications/{applicationId}/users")
                        .and(accept(MediaType.APPLICATION_JSON)), handler::getApplicationUsers)
                .andRoute(GET("/applications/clients/{clientId}/users/{userId}")
                        .and(accept(MediaType.APPLICATION_JSON)), handler::getClientRoleGroupNames);
    }
}
