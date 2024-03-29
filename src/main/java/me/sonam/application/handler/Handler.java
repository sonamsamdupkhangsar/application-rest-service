package me.sonam.application.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class Handler {
    private static final Logger LOG = LoggerFactory.getLogger(Handler.class);

    @Autowired
    private ApplicationBehavior applicationBehavior;

    public Mono<ServerResponse> getApplications(ServerRequest serverRequest) {
        LOG.info("get applications");
        Pageable pageable = Util.getPageable(serverRequest);

        return applicationBehavior.getApplications(pageable)
                .flatMap(s -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(s))
                .onErrorResume(throwable -> {
                    LOG.error("get applications call failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(getMap(Pair.of("error", throwable.getMessage())));
                });
    }

    public Mono<ServerResponse> getApplication(ServerRequest serverRequest) {
        LOG.info("get application");

        return applicationBehavior.getApplicationById(UUID.fromString(serverRequest.pathVariable("id")))
                .flatMap(s -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(s))
                .onErrorResume(throwable -> {
                    LOG.error("get application by id failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(getMap(Pair.of("error", throwable.getMessage())));
                });
    }

    public Mono<ServerResponse> getOrganizationApplications(ServerRequest serverRequest) {
        LOG.info("get organization applications");
        Pageable pageable = Util.getPageable(serverRequest);

        return applicationBehavior.getOrganizationApplications(
                UUID.fromString(serverRequest.pathVariable("organizationId")), pageable)
                .flatMap(s -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(s))
                .onErrorResume(throwable -> {
                    LOG.error("get organization applications call failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(getMap(Pair.of("error", throwable.getMessage())));
                });
    }

    public Mono<ServerResponse> createApplication(ServerRequest serverRequest) {
        LOG.info("create application");

        return applicationBehavior.createApplication(serverRequest.bodyToMono(ApplicationBody.class))
                .flatMap(s -> {
                        Map<String, String> map = new HashMap<>();
                        map.put("id", s);
                        return ServerResponse.created(URI.create("/applications/" + s))
                                .contentType(MediaType.APPLICATION_JSON).bodyValue(map);
                    }
                )
                .onErrorResume(throwable -> {
                    LOG.error("create failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(getMap(Pair.of("error", throwable.getMessage())));
                });
    }

    public Mono<ServerResponse> updateApplication(ServerRequest serverRequest) {
        LOG.info("update application");


        return applicationBehavior.updateApplication(serverRequest.bodyToMono(ApplicationBody.class))
                .flatMap(s -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(s))
                .onErrorResume(throwable -> {
                    LOG.error("update application failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(getMap(Pair.of("error", throwable.getMessage())));
                });
    }

    public Mono<ServerResponse> deleteApplication(ServerRequest serverRequest) {
        LOG.info("delete applicationUser");

        return applicationBehavior.deleteApplication(UUID.fromString(serverRequest.pathVariable("id")))
                .flatMap(s -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(s))
                .onErrorResume(throwable -> {
                    LOG.error("delete application failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(getMap(Pair.of("error", throwable.getMessage())));
                });
    }

    public Mono<ServerResponse> updateApplicationUsers(ServerRequest serverRequest) {
        LOG.info("add user");

        return applicationBehavior.updateApplicationUsers(serverRequest.bodyToFlux(ApplicationUserBody.class))
                .flatMap(s -> ServerResponse.ok().contentType(MediaType.TEXT_PLAIN).bodyValue(s))
                .onErrorResume(throwable -> {
                    LOG.error("update applicationusers failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.TEXT_PLAIN)
                            .bodyValue(getMap(Pair.of("error", throwable.getMessage())));
                });
    }

    public Mono<ServerResponse> getApplicationUsers(ServerRequest serverRequest) {
        LOG.info("get application users");

        Pageable pageable = Util.getPageable(serverRequest);

        return applicationBehavior.getApplicationUsers(UUID.fromString(serverRequest.pathVariable("id")), pageable)
                .flatMap(s -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(s))
                .onErrorResume(throwable -> {
                    LOG.error("get application user call failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(getMap(Pair.of("error", throwable.getMessage())));
                });
    }

    public Mono<ServerResponse> getClientRoleGroupNames(ServerRequest serverRequest) {
        LOG.info("get role based on client userId");

        return applicationBehavior.getClientRoleGroupNames(serverRequest.pathVariable("clientId"),
                UUID.fromString(serverRequest.pathVariable("userId")))
                .flatMap(s -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(s))
                .onErrorResume(throwable -> {
                    LOG.error("get role method failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(getMap(Pair.of("error", throwable.getMessage())));
                });
    }

    private Map<String, String> getMap(Pair<String, String>... pairs){
        Map<String, String> map = new HashMap<>();

        for(Pair<String, String> pair: pairs) {
            map.put(pair.getFirst(), pair.getSecond());
        }
        return map;
    }
}