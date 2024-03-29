package me.sonam.application.handler;

import me.sonam.application.handler.model.RoleGroupNames;
import me.sonam.application.repo.entity.Application;
import me.sonam.application.repo.entity.ApplicationUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ApplicationBehavior {
    Mono<Page<Application>> getApplications(Pageable pageable);
    Mono<Application> getApplicationById(UUID applicationId);
    Mono<Page<Application>> getOrganizationApplications(UUID organizationId, Pageable pageable);
    Mono<String> createApplication(Mono<ApplicationBody> applicationBodyMono);
    Mono<String> updateApplication(Mono<ApplicationBody> applicationBodyMono);
    Mono<String> deleteApplication(UUID applicationId);
    Mono<String> updateApplicationUsers(Flux<ApplicationUserBody> applicationUserBodyMono);
    Mono<Page<ApplicationUser>> getApplicationUsers(UUID applicationId, Pageable pageable);
    Mono<RoleGroupNames> getClientRoleGroupNames(String clientId, UUID userId);

}
