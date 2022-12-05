package me.sonam.application.repo;


import me.sonam.application.repo.entity.Application;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ApplicationRepository extends ReactiveCrudRepository<Application, UUID> {
    Flux<Application> findAllBy(Pageable pageable);
    Flux<Application> findAllByOrganizationId(UUID orgainzationId, Pageable pageable);
    Mono<Integer> countByOrganizationId(UUID organizationId);
    Mono<Application> findByClientId(UUID clientId);
}
