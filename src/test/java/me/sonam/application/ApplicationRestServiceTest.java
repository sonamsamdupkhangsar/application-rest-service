package me.sonam.application;

import me.sonam.application.handler.ApplicationBody;
import me.sonam.application.handler.ApplicationUserBody;
import me.sonam.application.handler.UserUpdate;
import me.sonam.application.repo.ApplicationRepository;
import me.sonam.application.repo.ApplicationUserRepository;
import me.sonam.application.repo.entity.ApplicationUser;
import org.junit.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


@EnableAutoConfiguration
@ExtendWith(SpringExtension.class)
@SpringBootTest( webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(MockitoExtension.class)
public class ApplicationRestServiceTest {
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationRestServiceTest.class);

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApplicationUserRepository applicationUserRepository;

    @Autowired
    private WebTestClient webTestClient;

    @Before
    public void setUp() {
        LOG.info("setup mock");
        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void deleteALl() {
        applicationRepository.deleteAll().subscribe(unused -> LOG.info("deleted all applications"));
        applicationUserRepository.deleteAll().subscribe(unused -> LOG.info("deleted all application users"));
    }

    @Test
    public void createApplication() {
        LOG.info("create application");
        UUID clientId = UUID.randomUUID();
        UUID creatorUserId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        ApplicationBody applicationBody = new ApplicationBody(null, "Baggy Pants Company",clientId.toString(), creatorUserId, organizationId);
        EntityExchangeResult<String> result = webTestClient.post().uri("/applications").bodyValue(applicationBody)
                .exchange().expectStatus().isCreated().expectBody(String.class).returnResult();

        LOG.info("result: {}", result.getResponseBody());
        assertThat(result.getResponseBody()).isNotEmpty();

        UUID id = UUID.fromString(result.getResponseBody());

        applicationRepository.findById(id)
                .subscribe(application -> LOG.info("found application with id: {}", application));

        LOG.info("verify application can be retrieved");

        result = webTestClient.get().uri("/applications").exchange().expectStatus().isOk().expectBody(String.class)
                .returnResult();

        LOG.info("page result contains: {}", result);

        applicationBody = new ApplicationBody(id, "New Name", clientId.toString(), creatorUserId, organizationId);
        result = webTestClient.put().uri("/applications").bodyValue(applicationBody)
                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();

        LOG.info("result from update: {}", result.getResponseBody());
        assertThat(result.getResponseBody()).isEqualTo(applicationBody.getId().toString());

        applicationRepository.findById(id)
                .subscribe(application -> LOG.info("found application with id: {}", application));



        //UID id, UUID applicationId, List< UserUpdate > userUpdates
        UserUpdate userUpdates1 = new UserUpdate(UUID.randomUUID(), ApplicationUser.RoleNamesEnum.admin.name(), UserUpdate.UpdateAction.add.name());
        UserUpdate userUpdates2 = new UserUpdate(UUID.randomUUID(), ApplicationUser.RoleNamesEnum.admin.name(), UserUpdate.UpdateAction.add.name());
        UserUpdate userUpdates3 = new UserUpdate(UUID.randomUUID(), ApplicationUser.RoleNamesEnum.user.name(), UserUpdate.UpdateAction.add.name());

        //leave null for id to generate its own
        ApplicationUserBody applicationUserBody = new ApplicationUserBody(null, id,
                Arrays.asList(userUpdates1, userUpdates2, userUpdates3));
        LOG.info("add user to application");

        result = webTestClient.put().uri("/applications/users").bodyValue(applicationUserBody)
                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();
        LOG.info("result: {}", result.getResponseBody());

        LOG.info("update 2 users to user level and delete one");
        userUpdates1 = new UserUpdate(userUpdates1.getUserId(), ApplicationUser.RoleNamesEnum.user.name(), UserUpdate.UpdateAction.update.name());
        userUpdates2 = new UserUpdate(userUpdates2.getUserId(), ApplicationUser.RoleNamesEnum.user.name(), UserUpdate.UpdateAction.update.name());
        userUpdates3 = new UserUpdate(userUpdates3.getUserId(), null, UserUpdate.UpdateAction.delete.name());

        //leave id null but pass in application id 'id'
        applicationUserBody = new ApplicationUserBody(null, id,
                Arrays.asList(userUpdates1, userUpdates2, userUpdates3));
        LOG.info("add user to application");

        result = webTestClient.put().uri("/applications/users").bodyValue(applicationUserBody)
                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();
        LOG.info("result: {}", result.getResponseBody());

        LOG.info("get all users in application {}", id);
        result = webTestClient.get().uri("/applications/"+id+"/users")
                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();
        LOG.info("result: {}", result.getResponseBody());

        result = webTestClient.get().uri("applications/"+organizationId).exchange().expectStatus().isOk()
                .expectBody(String.class).returnResult();
        LOG.info("got page results for applications by organizations: {}", result);

        result = webTestClient.delete().uri("/applications/"+id).exchange().expectStatus().isOk().expectBody(String.class)
                .returnResult();
        assertThat(result.getResponseBody()).isEqualTo("application deleted");

        StepVerifier.create(applicationRepository.existsById(id)).expectNext(false).expectComplete();
        applicationRepository.existsById(id).subscribe(aBoolean -> LOG.info("should be false after deletion: {}",aBoolean));
    }


}