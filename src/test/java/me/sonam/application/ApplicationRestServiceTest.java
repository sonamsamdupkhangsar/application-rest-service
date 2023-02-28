package me.sonam.application;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.sonam.application.handler.ApplicationBody;
import me.sonam.application.handler.ApplicationUserBody;
import me.sonam.application.handler.model.RoleGroupNames;
import me.sonam.application.repo.ApplicationRepository;
import me.sonam.application.repo.ApplicationUserRepository;
import me.sonam.application.repo.entity.ApplicationUser;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Before;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@AutoConfigureWebTestClient
@EnableAutoConfiguration
@SpringBootTest( classes = SpringApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(MockitoExtension.class)
public class ApplicationRestServiceTest {
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationRestServiceTest.class);

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApplicationUserRepository applicationUserRepository;

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    ReactiveJwtDecoder jwtDecoder;
    final static String clientId = "azudp31223";
    private static final String hmacKeyEndpoint = "http://localhost:{port}/jwts/hmacKey/"+clientId;
    private static MockWebServer mockWebServer;


    @Before
    public void setUp() {
        LOG.info("setup mock");
        MockitoAnnotations.openMocks(this);
    }
    @BeforeAll
    static void setupMockWebServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        LOG.info("host: {}, port: {}", mockWebServer.getHostName(), mockWebServer.getPort());
    }
    @AfterAll
    public static void shutdownMockWebServer() throws IOException {
        LOG.info("shutdown and close mockWebServer");
        mockWebServer.shutdown();
        mockWebServer.close();
    }
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) throws IOException {
        r.add("hmacKeyEndpoint", () -> hmacKeyEndpoint.replace("{port}", mockWebServer.getPort() + ""));
    }

    @AfterEach
    public void deleteALl() {
        applicationRepository.deleteAll().subscribe(unused -> LOG.info("deleted all applications"));
        applicationUserRepository.deleteAll().subscribe(unused -> LOG.info("deleted all application users"));
    }

    @Test
    public void createApplication() throws InterruptedException {
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        LOG.info("create application");
        //UUID clientId = UUID.randomUUID();

        UUID creatorUserId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        final String hmacKeyEndpointResponse = "{\n" +
                "  \"clientId\": \"azudp31223\",\n" +
                "  \"hmacMD5Algorithm\": \"HmacSHA256\",\n" +
                "  \"secretKey\": \"1234secret\"\n" +
                "}";
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(hmacKeyEndpointResponse));

        ApplicationBody applicationBody = new ApplicationBody(null, "Baggy Pants Company",clientId.toString(), creatorUserId, organizationId, ApplicationUser.RoleNamesEnum.user.name(), "");
        EntityExchangeResult<Map> createResult = webTestClient.post().uri("/applications").headers(addJwt(jwt)).bodyValue(applicationBody)
                .exchange().expectStatus().isCreated().expectBody(Map.class).returnResult();

        LOG.info("result from post application: {}", createResult.getResponseBody());
        StepVerifier.create(applicationRepository.existsByClientId(applicationBody.getClientId())).assertNext(aBoolean ->
                {
                    LOG.info("assert application with clientId exists: {}", aBoolean);
                    assertThat(aBoolean).isTrue();
                }).verifyComplete();


        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");

        UUID applicationId = UUID.fromString(createResult.getResponseBody().get("id").toString());
        LOG.info("result: {}", createResult.getResponseBody());
        assertThat(createResult.getResponseBody().get("id")).isNotNull();

        LOG.info("get applications by id {} and all users in it", applicationId);
        EntityExchangeResult<RestPage> usersResult = webTestClient.get().uri("/applications/"+applicationId+"/users")
                .headers(addJwt(jwt))
                .exchange().expectStatus().isOk().expectBody(RestPage.class).returnResult();

        LOG.info("pageResult pageable {}", usersResult.getResponseBody().getPageable());
        LOG.info("assert that only applicationuser exists");
        assertThat(usersResult.getResponseBody().getContent().size()).isEqualTo(1);
        LOG.info("applicationUser: {}", usersResult.getResponseBody().getContent().get(0));
        Object obj = usersResult.getResponseBody().getContent().get(0);
        LOG.info("obj.class {}, obj: {}", obj.getClass(), obj);
        LinkedHashMap linkedHashMap = (LinkedHashMap) obj;
        assertThat(linkedHashMap.get("userRole")).isEqualTo("user");

        usersResult.getResponseBody().getContent().forEach(o -> LOG.info("object: {}", o));

        //mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(hmacKeyEndpointResponse));
        LOG.info("trying to send the same payload leads to an error because clientId has already been used");
        webTestClient.post().uri("/applications").headers(addJwt(jwt)).bodyValue(applicationBody)
                .exchange().expectStatus().is4xxClientError().expectBody(String.class).
                consumeWith(stringEntityExchangeResult -> LOG.info("response: {}", stringEntityExchangeResult.getResponseBody()));

        //request = mockWebServer.takeRequest();
        //assertThat(request.getMethod()).isEqualTo("POST");

        LOG.info("second call with the same applicationBody should produce the results with ApplicationUser");
        usersResult = webTestClient.get().uri("/applications/"+applicationId+"/users")
                .headers(addJwt(jwt))
                .exchange().expectStatus().isOk().expectBody(RestPage.class).returnResult();

        LOG.info("pageResult pageable {}", usersResult.getResponseBody().getPageable());
        LOG.info("assert that only applicationuser exists");
        assertThat(usersResult.getResponseBody().getContent().size()).isEqualTo(1);
        LOG.info("applicationUser: {}", usersResult.getResponseBody().getContent().get(0));
        obj = usersResult.getResponseBody().getContent().get(0);
        LOG.info("obj.class {}, obj: {}", obj.getClass(), obj);
        linkedHashMap = (LinkedHashMap) obj;
        assertThat(linkedHashMap.get("userRole")).isEqualTo("user");

        applicationRepository.findById(applicationId)
                .subscribe(application -> LOG.info("found application with id: {}", application));

        LOG.info("verify application can be retrieved");

        EntityExchangeResult<String> result2 = webTestClient.get().uri("/applications").headers(addJwt(jwt)).exchange()
                .expectStatus().isOk().expectBody(String.class)
                .returnResult();

        LOG.info("page result contains: {}", result2);

        applicationBody = new ApplicationBody(applicationId, "New Name", clientId.toString(), creatorUserId, organizationId, ApplicationUser.RoleNamesEnum.admin.name(), "manager");
        EntityExchangeResult<String> result = webTestClient.put().uri("/applications").headers(addJwt(jwt)).bodyValue(applicationBody)
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

        result = webTestClient.put().uri("/applications/users").headers(addJwt(jwt)).bodyValue(applicationUserBodies)
                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();
        LOG.info("result: {}", result.getResponseBody());

        LOG.info("get applications by id and all users in it, which should give 4 applicationUsers");
        usersResult = webTestClient.get().uri("/applications/"+applicationId+"/users")
                .headers(addJwt(jwt))
                .exchange().expectStatus().isOk().expectBody(RestPage.class).returnResult();

        LOG.info("pageResult pageable {}", usersResult.getResponseBody().getPageable());
        LOG.info("assert that only applicationuser exists");
        assertThat(usersResult.getResponseBody().getContent().size()).isEqualTo(4);
        LOG.info("applicationUser: {}", usersResult.getResponseBody().getContent().get(0));
        usersResult.getResponseBody().getContent().forEach(o -> {
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
        obj = usersResult.getResponseBody().getContent().get(0);
        LOG.info("obj.class {}, obj: {}", obj.getClass(), obj);
        linkedHashMap = (LinkedHashMap) obj;
        assertThat(linkedHashMap.get("userRole")).isEqualTo("user");

        LOG.info("get applications by id and all users in it");
        EntityExchangeResult<RestPage> pageResult = webTestClient.get().uri("/applications/"+applicationId+"/users")
                .headers(addJwt(jwt))
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
                .headers(addJwt(jwt))
                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();
        LOG.info("update user add and delete result: {}", result.getResponseBody());

        LOG.info("get applications by id and all users in it, which should give 3 applicationUsers after deleting the userUpdate3");
        usersResult = webTestClient.get().uri("/applications/"+applicationId+"/users")
                .headers(addJwt(jwt)).exchange().expectStatus().isOk().expectBody(RestPage.class).returnResult();

        LOG.info("pageResult pageable {}", usersResult.getResponseBody().getPageable());
        LOG.info("assert that only applicationuser exists");
        assertThat(usersResult.getResponseBody().getContent().size()).isEqualTo(3);
        LOG.info("applicationUser: {}", usersResult.getResponseBody().getContent().get(0));
        usersResult.getResponseBody().getContent().forEach(o -> {
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
                .headers(addJwt(jwt)).exchange().expectStatus().isOk().expectBody(String.class).returnResult();
        LOG.info("result: {}", result.getResponseBody());

        LOG.info("get by organizationId: {}", organizationId);

        result = webTestClient.get().uri("/applications/organizations/"+organizationId)
                .headers(addJwt(jwt)).exchange().expectStatus().isOk()
                .expectBody(String.class).returnResult();
        LOG.info("got page results for applications by organizations: {}", result);


       EntityExchangeResult<Map> mapEntityExchangeResult = webTestClient.get().uri("/applications/clients/"+clientId+"/users/"+userId1)
                .exchange().expectStatus().isOk().expectBody(Map.class).returnResult();
       LOG.info("map contains: {}", mapEntityExchangeResult.getResponseBody());

       LOG.info("clientRolegroup with object");
        EntityExchangeResult<RoleGroupNames> clientRoleGroups = webTestClient.get().uri("/applications/clients/"+clientId+"/users/"+userId1)
                .exchange().expectStatus().isOk().expectBody(RoleGroupNames.class).returnResult();

        LOG.info("roleGroups: {}", clientRoleGroups.getResponseBody());
        marshalToJson(clientRoleGroups.getResponseBody());

        assertThat(clientRoleGroups.getResponseBody().getGroupNames().length).isEqualTo(2);
        LOG.info("groupNames: {}", clientRoleGroups.getResponseBody().getGroupNames());
        LOG.info("groupNames.length: {}", clientRoleGroups.getResponseBody().getGroupNames().length);
        LOG.info("groupNames: {}", clientRoleGroups.getResponseBody().getGroupNames());

        assertThat(clientRoleGroups.getResponseBody().getGroupNames()).contains("admin1touser", "employee");
        assertThat(clientRoleGroups.getResponseBody().getUserRole()).isEqualTo("user");

        clientRoleGroups = webTestClient.get().uri("/applications/clients/"+clientId+"/users/"+userId2)
                .exchange().expectStatus().isOk().expectBody(RoleGroupNames.class).returnResult();

        assertThat(clientRoleGroups.getResponseBody().getGroupNames()).isNull();
        assertThat(clientRoleGroups.getResponseBody().getUserRole()).isEqualTo("user");

        webTestClient.get().uri("/applications/clients/"+clientId+"/users/"+userId3)
                .exchange().expectStatus().is4xxClientError();

        result = webTestClient.delete().uri("/applications/"+applicationId).headers(addJwt(jwt))
                .exchange()
                .expectStatus().isOk().expectBody(String.class)
                .returnResult();
        assertThat(result.getResponseBody()).isEqualTo("application deleted");

        StepVerifier.create(applicationRepository.existsById(applicationId)).expectNext(false).expectComplete();
        applicationRepository.existsById(applicationId).subscribe(aBoolean -> LOG.info("should be false after deletion: {}",aBoolean));

        LOG.info("expect bad request after deleting the orgainzationId");
        result = webTestClient.get().uri("/applications/"+applicationId)
                .headers(addJwt(jwt)).exchange().expectStatus().isBadRequest()
                .expectBody(String.class).returnResult();
        LOG.info("got page results for applications by organizations: {}", result);
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

    /**
     * this will ensure application and applicationUser is rolledback when hmac clientId http callout creation
     * fails.
     * @throws InterruptedException
     */
    @Test
    public void createApplicationRollbackWhenHmacClientCreationFail() throws InterruptedException {
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        LOG.info("create application");

        UUID creatorUserId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        final String hmacKeyEndpointResponse = "{\n" +
                "  \"clientId\": \"azudp31223\",\n" +
                "  \"hmacMD5Algorithm\": \"HmacSHA256\",\n" +
                "  \"secretKey\": \"1234secret\"\n" +
                "}";
        mockWebServer.enqueue(new MockResponse().setResponseCode(400).setBody("no response"));

        ApplicationBody applicationBody = new ApplicationBody(null, "Baggy Pants Company", clientId.toString(), creatorUserId, organizationId, ApplicationUser.RoleNamesEnum.user.name(), "");
        EntityExchangeResult<Map> createResult = webTestClient.post().uri("/applications").headers(addJwt(jwt)).bodyValue(applicationBody)
                .exchange().expectStatus().isBadRequest().expectBody(Map.class).returnResult();

        LOG.info("result from post application: {}", createResult.getResponseBody());
        StepVerifier.create(applicationRepository.existsByClientId(applicationBody.getClientId())).assertNext(aBoolean ->
        {
            LOG.info("assert application with clientId exists: {}", aBoolean);
            assertThat(aBoolean).isFalse();
        }).verifyComplete();

    }
        @Test
    public void clientRoleGroupWhenNoClientId() {
        UUID clientIdNotExist = UUID.randomUUID();
        UUID userId1 = UUID.randomUUID();

        EntityExchangeResult<Map> mapEntityExchangeResult = webTestClient.get()
                .uri("/applications/clients/"+clientIdNotExist+"/users/"+userId1)
                .exchange().expectStatus().isBadRequest().expectBody(Map.class).returnResult();
        LOG.info("map contains: {}", mapEntityExchangeResult.getResponseBody());

    }

    private Jwt jwt(String subjectName) {
        return new Jwt("token", null, null,
                Map.of("alg", "none"), Map.of("sub", subjectName));
    }

    private Consumer<HttpHeaders> addJwt(Jwt jwt) {
        return headers -> headers.setBearerAuth(jwt.getTokenValue());
    }

    public void marshalToJson(Object object) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            final String json = objectMapper.writeValueAsString(object);
            LOG.info("json: {}", json);
        }
        catch (JsonProcessingException je) {
            LOG.error("failed to generate Json", je);
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