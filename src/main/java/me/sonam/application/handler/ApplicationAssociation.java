package me.sonam.application.handler;

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
    public Mono<String> createApplication(Mono<ApplicationBody> applicationBodyMono) {
        LOG.info("create application");

        return applicationBodyMono.flatMap(applicationBody -> applicationRepository.save(new Application(null, applicationBody.getName(), applicationBody.getClientId())))
                .map(application -> application.getId())
                .flatMap(uuid -> Mono.just(uuid.toString()));
    }

    @Override
    public Mono<String> updateApplication(Mono<ApplicationBody> applicationBodyMono) {
        LOG.info("update application");

        return applicationBodyMono.flatMap(applicationBody ->
            applicationRepository.save(new Application(applicationBody.getId(), applicationBody.getName(), applicationBody.getClientId())))
                .flatMap(application -> Mono.just(application.getId().toString()));
    }

    @Override
    public Mono<String> deleteApplication(UUID applicationId) {
        LOG.info("delete application");
        return applicationRepository.deleteById(applicationId).thenReturn("application deleted");
    }

    @Override
    public Mono<String> updateApplicationUsers(Mono<ApplicationUserBody> applicationUserBodyMono) {
        LOG.info("updated users in organization");

        return applicationUserBodyMono.doOnNext(applicationUserBody -> {
            LOG.info("save application user updates");
            applicationUserBody.getUserUpdateList().forEach(userUpdate -> {
                if (userUpdate.getUpdate().equals(UserUpdate.UpdateAction.add)) {
                    applicationUserRepository.existsByApplicationIdAndUserId(
                            applicationUserBody.getApplicationId(), userUpdate.getUserId())
                            .doOnNext(aBoolean -> LOG.info("exists by orgIdAndUserId already?: {}", aBoolean))
                            .filter(aBoolean -> !aBoolean)
                            .map(aBoolean -> new ApplicationUser
                                    (null, applicationUserBody.getApplicationId(), userUpdate.getUserId(),
                                            userUpdate.getRole()))
                            .flatMap(applicationUser -> applicationUserRepository.save(applicationUser))
                            .subscribe(organizationUser -> LOG.info("saved applicationUser"));


                } else if (userUpdate.getUpdate().equals(UserUpdate.UpdateAction.delete)) {
                    if (applicationUserBody.getId() != null) {
                        applicationUserRepository.existsById(applicationUserBody.getId())
                                .filter(aBoolean -> aBoolean)
                                .map(aBoolean -> applicationUserRepository.deleteById(applicationUserBody.getId()))
                                .subscribe(organizationUser -> LOG.info("deleted applicationUser"));
                    }
                    else {
                        LOG.info("deleting using userId and appId");
                        applicationUserRepository.deleteByApplicationIdAndUserId(
                                applicationUserBody.getApplicationId(), userUpdate.getUserId())
                                .subscribe(integer -> LOG.info("delted by applicationId and userId"));
                    }
                }
                else if (userUpdate.getUpdate().equals(UserUpdate.UpdateAction.update)) {
                    applicationUserRepository.findByApplicationIdAndUserId(
                            applicationUserBody.getApplicationId(), userUpdate.getUserId())
                            .switchIfEmpty(Mono.just(
                                    new ApplicationUser(null, applicationUserBody.getApplicationId(),
                                            userUpdate.getUserId(), userUpdate.getRole())))
                            .flatMap(applicationUser -> applicationUserRepository.save(applicationUser))
                            .subscribe(organizationUser -> LOG.info("updated applicationUser"));
                }
                else {
                    throw new ApplicationException("UserUpdate action invalid: " + userUpdate.getUpdate().name());
                }
            });
        }).thenReturn("applicationUser update done");
    }

    @Override
    public Mono<Page<ApplicationUser>> getApplicationUsers(UUID applicationId, Pageable pageable) {
        LOG.info("get users assigned to application");

        return applicationUserRepository.findByApplicationId(applicationId, pageable)
                .collectList()
                .zipWith(applicationUserRepository.countByApplicationId(applicationId))
                .map(objects -> new PageImpl<>(objects.getT1(), pageable, objects.getT2()));
    }
}
