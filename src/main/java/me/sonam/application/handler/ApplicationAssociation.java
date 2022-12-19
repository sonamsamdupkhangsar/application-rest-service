package me.sonam.application.handler;

import me.sonam.application.handler.model.RoleGroupNames;
import me.sonam.application.repo.ApplicationRepository;
import me.sonam.application.repo.ApplicationUserRepository;
import me.sonam.application.repo.entity.Application;
import me.sonam.application.repo.entity.ApplicationUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class ApplicationAssociation implements ApplicationBehavior {
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationAssociation.class);

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApplicationUserRepository applicationUserRepository;

    @Override
    public Mono<Page<Application>> getApplications(Pageable pageable) {
        LOG.info("get all applications");

        return applicationRepository.findAllBy(pageable).collectList()
                .zipWith(applicationRepository.count())
                .map(objects -> new PageImpl<>(objects.getT1(), pageable, objects.getT2()));
    }

    @Override
    public Mono<Application> getApplicationById(UUID applicationId) {
        LOG.info("get application byd id");
        return applicationRepository.findById(applicationId).
                switchIfEmpty(Mono.error(new ApplicationException("No application found with id")));

    }

    @Override
    public Mono<Page<Application>> getOrganizationApplications(UUID organizationId, Pageable pageable) {
        LOG.info("get organization applications");

        return applicationRepository.findAllByOrganizationId(organizationId, pageable).collectList()
                .zipWith(applicationRepository.countByOrganizationId(organizationId))
                .map(objects -> new PageImpl<>(objects.getT1(), pageable, objects.getT2()));
    }

    @Override
    public Mono<String> createApplication(Mono<ApplicationBody> applicationBodyMono) {
        LOG.info("create application");

        return applicationBodyMono.flatMap(applicationBody ->
                applicationRepository.existsByClientId(UUID.fromString(applicationBody.getClientId()))
                .filter(aBoolean -> !aBoolean)
                .switchIfEmpty(Mono.error(new ApplicationException("application with clientId already exists")))
                .map(aBoolean -> new Application(null, applicationBody.getName(),
                        applicationBody.getClientId(), applicationBody.getCreatorUserId(),
                        applicationBody.getOrganizationId()))
                        .flatMap(application -> applicationRepository.save(application).zipWith(Mono.just(applicationBody))))
                .map(objects ->
                        new ApplicationUser
                                (null, objects.getT1().getId(), objects.getT1().getCreatorUserId(), objects.getT2().getUserRole(),
                                        objects.getT2().getGroupNames()))
                .flatMap(applicationUser -> applicationUserRepository.save(applicationUser))
                .map(applicationUser -> applicationUser.getApplicationId())
                .flatMap(uuid -> Mono.just(uuid.toString()));
    }

    /**
     * this will only update the application itself, not the applicationusers
     * @param applicationBodyMono
     * @return
     */
    @Override
    public Mono<String> updateApplication(Mono<ApplicationBody> applicationBodyMono) {
        LOG.info("update application only, not the users");

        return applicationBodyMono.flatMap(applicationBody ->
            applicationRepository.save(new Application(applicationBody.getId(), applicationBody.getName(),
                    applicationBody.getClientId(), applicationBody.getCreatorUserId(), applicationBody.getOrganizationId())))
                .flatMap(application -> Mono.just(application.getId().toString()));
    }

    @Override
    public Mono<String> deleteApplication(UUID applicationId) {
        LOG.info("delete application");

        LOG.info("delete application users and then delete application");
        return applicationUserRepository.deleteByApplicationId(applicationId).then(
            applicationRepository.deleteById(applicationId).thenReturn("application deleted"));
    }

    @Override
    public Mono<String> updateApplicationUsers(Flux<ApplicationUserBody> applicationUserBodyFlux) {
        LOG.info("updated users in organization");

        return applicationUserBodyFlux.doOnNext(applicationUserBody -> {
            LOG.info("save application user updates");

            if (applicationUserBody.getUpdateAction().equals(ApplicationUserBody.UpdateAction.add)) {
                applicationUserRepository.existsByApplicationIdAndUserId(
                        applicationUserBody.getApplicationId(), applicationUserBody.getUserId())
                        .doOnNext(aBoolean -> LOG.info("exists by applicationIdAndUserId already?: {}", aBoolean))
                        .filter(aBoolean -> !aBoolean)
                        .map(aBoolean -> new ApplicationUser
                                (null, applicationUserBody.getApplicationId(), applicationUserBody.getUserId(),
                                        applicationUserBody.getUserRole(), applicationUserBody.getGroupNames()))
                        .flatMap(applicationUser -> applicationUserRepository.save(applicationUser))
                        .subscribe(organizationUser -> LOG.info("saved applicationUser"));

            } else if (applicationUserBody.getUpdateAction().equals(ApplicationUserBody.UpdateAction.delete)) {
                if (applicationUserBody.getId() != null) {
                    applicationUserRepository.existsById(applicationUserBody.getId())
                            .filter(aBoolean -> aBoolean)
                            .map(aBoolean -> applicationUserRepository.deleteById(applicationUserBody.getId()))
                            .subscribe(organizationUser -> LOG.info("deleted applicationUser"));
                }
                else {
                    LOG.info("deleting using userId and appId");
                    applicationUserRepository.deleteByApplicationIdAndUserId(
                            applicationUserBody.getApplicationId(), applicationUserBody.getUserId())
                            .subscribe(rows -> LOG.info("deleted by applicationId and userId: {}", rows));
                }
            }
            else if (applicationUserBody.getUpdateAction().equals(ApplicationUserBody.UpdateAction.update)) {
                //in update the applicationUser with appId and userId must existacc
                applicationUserRepository.findByApplicationIdAndUserId(
                        applicationUserBody.getApplicationId(), applicationUserBody.getUserId())
                        .map(applicationUser ->  new ApplicationUser(applicationUser.getId()
                                , applicationUser.getApplicationId(), applicationUser.getUserId()
                                , applicationUserBody.getUserRole(), applicationUserBody.getGroupNames()))
                        .doOnNext(applicationUser -> {
                            applicationUserRepository.countByApplicationId(applicationUser.getApplicationId()).subscribe(aLong -> LOG.info("count of applicationUser: {}", aLong));

                        })
                        .flatMap(applicationUser -> applicationUserRepository.save(applicationUser))
                        .subscribe(organizationUser -> LOG.info("updated applicationUser"));
            }
            else {
                throw new ApplicationException("UserUpdate action invalid: " + applicationUserBody.getUpdateAction().name());
            }
        }).then(Mono.just("applicationUser update done"));
    }


    @Override
    public Mono<Page<ApplicationUser>> getApplicationUsers(UUID applicationId, Pageable pageable) {
        LOG.info("get users assigned to application");

        return applicationUserRepository.findByApplicationId(applicationId, pageable)
                .collectList()
                .zipWith(applicationUserRepository.countByApplicationId(applicationId))
                .map(objects -> new PageImpl<>(objects.getT1(), pageable, objects.getT2()));
    }

    @Override
    public Mono<RoleGroupNames> getClientRoleGroupNames(UUID clientId, UUID userId) {
        LOG.info("get application role given a clientId {} and userId {}", clientId, userId);

        return applicationRepository.findByClientId(clientId)
                .switchIfEmpty(Mono.error(new ApplicationException("clientId not found")))

                .doOnNext(application -> LOG.info("applicaiton.id: {}", application.getId()))

                .flatMap(application -> applicationUserRepository.findByApplicationIdAndUserId(application.getId(), userId))
                .switchIfEmpty(Mono.error(new ApplicationException("no applicationUser found for applicationId and userId")))
                .map(applicationUser ->
                   new RoleGroupNames(applicationUser.getUserRole(), applicationUser.getGroupNames()));

    }
}
