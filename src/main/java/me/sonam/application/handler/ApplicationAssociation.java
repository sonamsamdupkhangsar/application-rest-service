package me.sonam.application.handler;

import me.sonam.application.handler.model.RoleGroupNames;
import me.sonam.application.repo.ApplicationRepository;
import me.sonam.application.repo.ApplicationUserRepository;
import me.sonam.application.repo.entity.Application;
import me.sonam.application.repo.entity.ApplicationUser;
import me.sonam.security.headerfilter.ReactiveRequestContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.UUID;

@Service
public class ApplicationAssociation implements ApplicationBehavior {
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationAssociation.class);

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApplicationUserRepository applicationUserRepository;

    @Autowired
    private ReactiveRequestContextHolder reactiveRequestContextHolder;
    @Value("${jwt-service.root}${jwt-service.hmacKey}")
    private String hmacKeyEndpoint;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @PostConstruct
    public void setWebClient() {
        webClientBuilder.filter(reactiveRequestContextHolder.headerFilter());
    }

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

        return saveApplicationAndUser(applicationBodyMono).flatMap(application-> {
            LOG.info("endpoint: {}", hmacKeyEndpoint+application.getClientId());
            LOG.info("create hmacKey for application with clientId: {}", application.getClientId());
            WebClient.ResponseSpec spec = webClientBuilder.build().post().uri(hmacKeyEndpoint+application.getClientId()).retrieve();

            return spec.bodyToMono(String.class).flatMap(s -> {
                LOG.info("activation response from jwt-rest-service hmac endpoint is: {}", s);
                return Mono.just(s);
            }).onErrorResume(throwable -> {
                LOG.error("error on jwt-rest-service call {}", throwable);

                applicationRepository.deleteByClientId(application.getClientId()).subscribe(integer ->
                        LOG.info("delete {} application by clientId", integer));
                applicationUserRepository.deleteByApplicationId(application.getId()).subscribe(
                        integer ->
                                LOG.info("delete {} applicationUser by applicationId", integer)
                );

                if (throwable instanceof WebClientResponseException) {
                    WebClientResponseException webClientResponseException = (WebClientResponseException) throwable;
                    LOG.error("error body contains: {}", webClientResponseException.getResponseBodyAsString());
                    return Mono.error(new ApplicationException("failed to hmackey for clientId, error: "+webClientResponseException.getResponseBodyAsString()));
                }
                else {
                    return Mono.error(new ApplicationException("jwt-rest-service hmackey creation api call failed with error: " +throwable.getMessage()));
                }
            }).thenReturn(application.getId().toString());
        });
    }

    public Mono<Application> saveApplicationAndUser(Mono<ApplicationBody> applicationBodyMono) {
        LOG.info("save application and applicationUser");

        return applicationBodyMono.flatMap(applicationBody ->
                        applicationRepository.existsByClientId(applicationBody.getClientId())
                                .flatMap(aBoolean -> {
                                    LOG.info("existByClientId: {}, true?: {}", applicationBody.getClientId(), aBoolean);
                                    if (aBoolean) {
                                        return Mono.error(new ApplicationException("application with clientId already exists"));
                                    }
                                    else {
                                        return Mono.just(aBoolean);
                                    }
                                })
                                .flatMap(aBoolean -> {
                                    var application = new Application(null, applicationBody.getName(),
                                        applicationBody.getClientId(), applicationBody.getCreatorUserId(),
                                        applicationBody.getOrganizationId());

                                    applicationRepository.save(application).subscribe(application1 -> LOG.info("saved application: {}", application1));
                                    return Mono.just(new ApplicationUser
                                            (null, application.getId(), application.getCreatorUserId(), applicationBody.getUserRole(),
                                                    applicationBody.getGroupNames()));
                                })
                .flatMap(applicationUser -> {
                    LOG.info("save applicationUser in repo");
                    applicationUserRepository.save(applicationUser).subscribe();
                    return applicationRepository.findById(applicationUser.getApplicationId());
                }));
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
    public Mono<RoleGroupNames> getClientRoleGroupNames(String clientId, UUID userId) {
        LOG.info("get application role given a clientId {} and userId {}", clientId, userId);

        return applicationRepository.findFirstByClientId(clientId)
                .switchIfEmpty(Mono.error(new ApplicationException("clientId not found")))
                .doOnNext(application -> LOG.info("application.id: {}", application.getId()))
                .flatMap(application -> applicationUserRepository.findByApplicationIdAndUserId(application.getId(), userId))
                .switchIfEmpty(Mono.error(new ApplicationException("no applicationUser found for applicationId and userId")))
                .map(applicationUser ->
                   new RoleGroupNames(applicationUser.getUserRole(), applicationUser.getGroupNames()));

    }
}
