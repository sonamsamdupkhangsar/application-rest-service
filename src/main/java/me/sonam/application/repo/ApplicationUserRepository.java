package me.sonam.application.repo;

import me.sonam.application.repo.entity.ApplicationUser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ApplicationUserRepository extends ReactiveCrudRepository<ApplicationUser, UUID> {
    Flux<ApplicationUser> findByApplicationId(UUID applicationId, Pageable pageable);
    Mono<Long> countByApplicationId(UUID applicationId);
    Mono<Boolean> existsByApplicationIdAndUserId(UUID applicationId, UUID userId);
    Mono<ApplicationUser> findByApplicationIdAndUserId(UUID applicationId, UUID userId);
    Mono<Integer> deleteByApplicationIdAndUserId(UUID applicationId, UUID userId);
    Mono<Integer> deleteByApplicationId(UUID applicationId);
}
