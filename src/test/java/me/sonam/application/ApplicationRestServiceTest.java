package me.sonam.application;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.sonam.application.handler.ApplicationBody;
import me.sonam.application.handler.ApplicationUserBody;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;


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

        ApplicationBody applicationBody = new ApplicationBody(null, "Baggy Pants Company",clientId.toString(), creatorUserId, organizationId, ApplicationUser.RoleNamesEnum.user.name(), "");
        EntityExchangeResult<String> result = webTestClient.post().uri("/applications").bodyValue(applicationBody)
                .exchange().expectStatus().isCreated().expectBody(String.class).returnResult();

        LOG.info("get applications by id and all users in it");
        EntityExchangeResult<RestPage> createdResult = webTestClient.get().uri("/applications/"+result.getResponseBody()+"/users")
                .exchange().expectStatus().isOk().expectBody(RestPage.class).returnResult();

        LOG.info("pageResult pageable {}", createdResult.getResponseBody().getPageable());
        LOG.info("assert that only applicationuser exists");
        assertThat(createdResult.getResponseBody().getContent().size()).isEqualTo(1);
        LOG.info("applicationUser: {}", createdResult.getResponseBody().getContent().get(0));
        Object obj = createdResult.getResponseBody().getContent().get(0);
        LOG.info("obj.class {}, obj: {}", obj.getClass(), obj);
        LinkedHashMap linkedHashMap = (LinkedHashMap) obj;
        assertThat(linkedHashMap.get("userRole")).isEqualTo("user");

        createdResult.getResponseBody().getContent().forEach(o -> LOG.info("object: {}", o));

        webTestClient.post().uri("/applications").bodyValue(applicationBody)
                .exchange().expectStatus().isCreated().expectBody(String.class).
                consumeWith(stringEntityExchangeResult -> LOG.info("response: {}", stringEntityExchangeResult.getResponseBody()));

        LOG.info("result: {}", result.getResponseBody());
        assertThat(result.getResponseBody()).isNotEmpty();

        LOG.info("second call with the same applicationBody should produce the results with ApplicationUser");
        createdResult = webTestClient.get().uri("/applications/"+result.getResponseBody()+"/users")
                .exchange().expectStatus().isOk().expectBody(RestPage.class).returnResult();

        LOG.info("pageResult pageable {}", createdResult.getResponseBody().getPageable());
        LOG.info("assert that only applicationuser exists");
        assertThat(createdResult.getResponseBody().getContent().size()).isEqualTo(1);
        LOG.info("applicationUser: {}", createdResult.getResponseBody().getContent().get(0));
        obj = createdResult.getResponseBody().getContent().get(0);
        LOG.info("obj.class {}, obj: {}", obj.getClass(), obj);
        linkedHashMap = (LinkedHashMap) obj;
        assertThat(linkedHashMap.get("userRole")).isEqualTo("user");

        UUID applicationId = UUID.fromString(result.getResponseBody());

        applicationRepository.findById(applicationId)
                .subscribe(application -> LOG.info("found application with id: {}", application));

        LOG.info("verify application can be retrieved");

        result = webTestClient.get().uri("/applications").exchange().expectStatus().isOk().expectBody(String.class)
                .returnResult();

        LOG.info("page result contains: {}", result);

        applicationBody = new ApplicationBody(applicationId, "New Name", clientId.toString(), creatorUserId, organizationId, ApplicationUser.RoleNamesEnum.admin.name(), "manager");
        result = webTestClient.put().uri("/applications").bodyValue(applicationBody)
                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();

        LOG.info("result from update: {}", result.getResponseBody());
        assertThat(result.getResponseBody()).isEqualTo(applicationBody.getId().toString());

        applicationRepository.findById(applicationId)
                .subscribe(application -> LOG.info("found application with id: {}", application));

        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        UUID userId3 = UUID.randomUUID();

        //leave null for id to generate its own
        List<ApplicationUserBody> applicationUserBodies = Arrays.asList(new ApplicationUserBody(null,
                applicationId, userId1, ApplicationUserBody.UpdateAction.add,
                        ApplicationUser.RoleNamesEnum.admin.name(),"admin1, employee"),
                new ApplicationUserBody(null,
                        applicationId, userId2, ApplicationUserBody.UpdateAction.add,
                        ApplicationUser.RoleNamesEnum.admin.name(),"admin2, employee"),
                new ApplicationUserBody(null,
                        applicationId, userId3, ApplicationUserBody.UpdateAction.add,
                        ApplicationUser.RoleNamesEnum.user.name(),null));

        LOG.info("add users to application");

        result = webTestClient.put().uri("/applications/users").bodyValue(applicationUserBodies)
                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();
        LOG.info("result: {}", result.getResponseBody());

        LOG.info("get applications by id and all users in it, which should give 4 applicationUsers");
        createdResult = webTestClient.get().uri("/applications/"+applicationId+"/users")
                .exchange().expectStatus().isOk().expectBody(RestPage.class).returnResult();

        LOG.info("pageResult pageable {}", createdResult.getResponseBody().getPageable());
        LOG.info("assert that only applicationuser exists");
        assertThat(createdResult.getResponseBody().getContent().size()).isEqualTo(4);
        LOG.info("applicationUser: {}", createdResult.getResponseBody().getContent().get(0));
        createdResult.getResponseBody().getContent().forEach(o -> {
            LinkedHashMap<String, String> linkedHashMap1 = (LinkedHashMap) o;

            LOG.info("linkedHashMap1: {}", linkedHashMap1);

            if (linkedHashMap1.get("userId").toString().equals(userId1.toString())) {
                assertThat(linkedHashMap1.get("userRole")).isEqualTo("admin");
                LOG.info("verified is admin for userUpdate 1");
            }
            else if (linkedHashMap1.get("userId").toString().equals(userId2.toString())) {
                assertThat(linkedHashMap1.get("userRole")).isEqualTo("admin");
                LOG.info("verified is admin for userUpdate 2");
            }
            else if (linkedHashMap1.get("userId").toString().equals(userId3.toString())) {
                assertThat(linkedHashMap1.get("userRole")).isEqualTo("user");
                LOG.info("verified is user for userUpdate 3");
            }
            else {
                assertThat(linkedHashMap1.get("userRole").toString()).isEqualTo("user");
                LOG.info("verified is user for userUpdate from initialization");
            }

        });
        obj = createdResult.getResponseBody().getContent().get(0);
        LOG.info("obj.class {}, obj: {}", obj.getClass(), obj);
        linkedHashMap = (LinkedHashMap) obj;
        assertThat(linkedHashMap.get("userRole")).isEqualTo("user");

        LOG.info("get applications by id and all users in it");
        EntityExchangeResult<RestPage> pageResult = webTestClient.get().uri("/applications/"+applicationId+"/users")
                .exchange().expectStatus().isOk().expectBody(RestPage.class).returnResult();
        //LOG.info("result: {}", result.getResponseBody());
        LOG.info("pageResult pageable {}", pageResult.getResponseBody().getPageable());
        pageResult.getResponseBody().getContent().forEach(o -> LOG.info("object: {}", o));

        LOG.info("update 2 users to user level and delete one");

        //leave null for id to generate its own
        applicationUserBodies = Arrays.asList(new ApplicationUserBody(null,
                        applicationId, userId1, ApplicationUserBody.UpdateAction.update,
                        ApplicationUser.RoleNamesEnum.user.name(),"admin1touser, employee"),
                new ApplicationUserBody(null,
                        applicationId, userId2, ApplicationUserBody.UpdateAction.update,
                        ApplicationUser.RoleNamesEnum.user.name(),null),
                new ApplicationUserBody(null,
                        applicationId, userId3, ApplicationUserBody.UpdateAction.delete,
                        null,"manager2"));

        //leave id null but pass in application id 'id'
       /* applicationUserBody = new ApplicationUserBody(null, applicationId,
                Arrays.asList(userUpdates4, userUpdates5, userUpdates6));*/
        LOG.info("add user to application");

        result = webTestClient.put().uri("/applications/users").bodyValue(applicationUserBodies)
                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();
        LOG.info("update user add and delete result: {}", result.getResponseBody());

        LOG.info("get applications by id and all users in it, which should give 3 applicationUsers after deleting the userUpdate3");
        createdResult = webTestClient.get().uri("/applications/"+applicationId+"/users")
                .exchange().expectStatus().isOk().expectBody(RestPage.class).returnResult();

        LOG.info("pageResult pageable {}", createdResult.getResponseBody().getPageable());
        LOG.info("assert that only applicationuser exists");
        assertThat(createdResult.getResponseBody().getContent().size()).isEqualTo(3);
        LOG.info("applicationUser: {}", createdResult.getResponseBody().getContent().get(0));
        createdResult.getResponseBody().getContent().forEach(o -> {
            LinkedHashMap<String, String> linkedHashMap1 = (LinkedHashMap) o;

            LOG.info("linkedHashMap1: {}", linkedHashMap1);

            if (linkedHashMap1.get("userId").toString().equals(userId1.toString())) {
                assertThat(linkedHashMap1.get("userRole")).isEqualTo("user");
                LOG.info("verified is changed from admin to user for userUpdate 1");
            }
            else if (linkedHashMap1.get("userId").toString().equals(userId2.toString())) {
                assertThat(linkedHashMap1.get("userRole")).isEqualTo("user");
                LOG.info("verified is changed from admin to user for userUpdate 2");
            }
            else if (linkedHashMap1.get("userId").toString().equals(userId3.toString())) {
                fail("this should not happen as userUpdate3 is now deleted after update");
            }
            else {
                assertThat(linkedHashMap1.get("userRole").toString()).isEqualTo("user");
                LOG.info("verified is user from initialization");
            }

        });

        LOG.info("get all users in application {}", applicationId);
        result = webTestClient.get().uri("/applications/"+applicationId+"/users")
                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();
        LOG.info("result: {}", result.getResponseBody());

        result = webTestClient.get().uri("applications/"+organizationId).exchange().expectStatus().isOk()
                .expectBody(String.class).returnResult();
        LOG.info("got page results for applications by organizations: {}", result);

        result = webTestClient.delete().uri("/applications/"+applicationId).exchange().expectStatus().isOk().expectBody(String.class)
                .returnResult();
        assertThat(result.getResponseBody()).isEqualTo("application deleted");

        StepVerifier.create(applicationRepository.existsById(applicationId)).expectNext(false).expectComplete();
        applicationRepository.existsById(applicationId).subscribe(aBoolean -> LOG.info("should be false after deletion: {}",aBoolean));
    }

    private ApplicationUser getApplicationUser(String string) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(string, ApplicationUser.class);
        }
        catch (Exception e) {
            LOG.error("failed to marshal to ApplicationUser", e);
            return null;
        }
    }

}
@JsonIgnoreProperties(ignoreUnknown = true, value = {"pageable"})
class RestPage<T> extends PageImpl<T> {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RestPage(@JsonProperty("content") List<T> content,
                    @JsonProperty("number") int page,
                    @JsonProperty("size") int size,
                    @JsonProperty("totalElements") long total,
                    @JsonProperty("numberOfElements") int numberOfElements,
                    @JsonProperty("pageNumber") int pageNumber
    ) {
        super(content, PageRequest.of(page, size), total);
    }

    public RestPage(Page<T> page) {
        super(page.getContent(), page.getPageable(), page.getTotalElements());
    }


}