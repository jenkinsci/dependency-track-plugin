/*
 * Copyright 2020 OWASP.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jenkinsci.plugins.DependencyTrack;

import hudson.FilePath;
import hudson.util.VersionNumber;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import net.sf.json.JSONObject;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jenkinsci.plugins.DependencyTrack.model.Project;
import org.jenkinsci.plugins.DependencyTrack.model.UploadResult;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner.StrictStubs;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
@RunWith(StrictStubs.class)
public class ApiClientTest {
    
    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    private static final String API_KEY = "api-key";

    private DisposableServer server;

    @Mock
    private ConsoleLogger logger;

    @After
    public void tearDown() {
        if (server != null) {
            server.disposeNow();
        }
    }
    
    private ApiClient createClient() {
        return new ApiClient(String.format("http://%s:%d", server.host(), server.port()), API_KEY, logger, 1, 1);
    }

    @Test
    public void testConnectionTest() throws ApiClientException {
        server = HttpServer.create()
                // no ipv6 due to https://bugs.openjdk.java.net/browse/JDK-8220663
                .host("localhost")
                .port(0)
                .route(routes -> routes.get(ApiClient.PROJECT_URL, (request, response) -> {
                    assertThat(request.requestHeaders().contains(ApiClient.API_KEY_HEADER, API_KEY, false)).isTrue();
                    assertThat(request.requestHeaders().contains(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON, true)).isTrue();
                    return response.addHeader("X-Powered-By", "Dependency-Track v3.8.0").send();
                }))
                .bindNow();

        ApiClient uut = createClient();

        assertThat(uut.testConnection()).isEqualTo("Dependency-Track v3.8.0");
    }

    @Test
    public void testConnectionTestUnauth() {
        server = HttpServer.create()
                .host("localhost")
                .port(0)
                .route(routes -> routes.get(ApiClient.PROJECT_URL, (request, response) -> response.status(HttpResponseStatus.UNAUTHORIZED).send()))
                .bindNow();

        ApiClient uut = createClient();

        assertThatCode(() -> uut.testConnection()).isInstanceOf(ApiClientException.class)
                .hasNoCause()
                .hasMessage(Messages.ApiClient_Error_Connection(401, "Unauthorized"));
    }

    @Test
    public void testConnectionTestInternalError() {
        server = HttpServer.create()
                .host("localhost")
                .port(0)
                .route(routes -> routes.get(ApiClient.PROJECT_URL, (request, response) -> response.status(HttpResponseStatus.INTERNAL_SERVER_ERROR).sendString(Mono.just("something went wrong"))))
                .bindNow();

        ApiClient uut = createClient();

        assertThatCode(() -> uut.testConnection()).isInstanceOf(ApiClientException.class)
                .hasNoCause()
                .hasMessage(Messages.ApiClient_Error_Connection(500, "Internal Server Error"));
        
        verify(logger).log("something went wrong");
    }

    @Test()
    public void getProjectsTest() throws ApiClientException {
        server = HttpServer.create()
                .host("localhost")
                .port(0)
                .route(routes -> routes.get(ApiClient.PROJECT_URL, (request, response) -> {
                    assertThat(request.requestHeaders().contains(ApiClient.API_KEY_HEADER, API_KEY, false)).isTrue();
                    assertThat(request.requestHeaders().contains(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON, true)).isTrue();
                    QueryStringDecoder query = new QueryStringDecoder(request.uri());
                    assertThat(query.parameters())
                            .contains(entry("limit", Collections.singletonList("500")), entry("excludeInactive", Collections.singletonList("true")))
                            .containsKey("page").extractingByKey("page", as(InstanceOfAssertFactories.list(String.class)))
                            .hasSize(1).first().satisfies(p -> {
                        assertThat(Integer.valueOf(p)).isBetween(1, 3);
                    });
                    int page = Integer.valueOf(query.parameters().get("page").get(0));
                    switch (page) {
                        case 1:
                            return response.sendString(Mono.just("[{\"name\":\"Project 1\",\"uuid\":\"uuid-1\",\"version\":null},{\"name\":\"Project 2\",\"uuid\":\"uuid-2\",\"version\":\"null\"}]"));
                        case 2:
                            return response.sendString(Mono.just("[{\"name\":\"Project 3\",\"uuid\":\"uuid-3\",\"version\":\"1.2.3\",\"lastBomImportStr\":\"2007-12-03T10:15:30\",\"tags\":[{\"name\":\"tag1\"},{\"name\":\"tag2\"},{\"name\":null}]}]"));
                    }
                    return response.sendNotFound();
                }))
                .bindNow();

        ApiClient uut = createClient();
        final List<Project> projects = uut.getProjects();
        // org.kohsuke.stapler.json-lib before version 2.4-jenkins-3 treated the string "null" as {@code JSONNull}, which was not JSON-spec compliant
        boolean isOldJsonVersion = net.sf.json.JSONNull.getInstance().equals("null");

        assertThat(projects).containsExactly(
                Project.builder().name("Project 1").uuid("uuid-1").tags(Collections.emptyList()).build(),
                Project.builder().name("Project 2").uuid("uuid-2").version(isOldJsonVersion ? null : "null").tags(Collections.emptyList()).build(),
                Project.builder().name("Project 3").uuid("uuid-3").version("1.2.3").tags(Arrays.asList("tag1", "tag2")).lastBomImport(LocalDateTime.of(2007, Month.DECEMBER, 3, 10, 15, 30)).build()
        );
    }

    @Test
    public void lookupProjectTest() throws ApiClientException {
        String projectName = "test-project";
        String projectVersion = "1.2.3";
        server = HttpServer.create()
                .host("localhost")
                .port(0)
                .route(routes -> routes.get(ApiClient.PROJECT_LOOKUP_URL, (request, response) -> {
                    assertThat(request.requestHeaders().contains(ApiClient.API_KEY_HEADER, API_KEY, false)).isTrue();
                    assertThat(request.requestHeaders().contains(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON, true)).isTrue();
                    QueryStringDecoder query = new QueryStringDecoder(request.uri());
                    assertThat(query.parameters())
                            .containsExactly(
                                    entry(ApiClient.PROJECT_LOOKUP_NAME_PARAM, Collections.singletonList(projectName)),
                                    entry(ApiClient.PROJECT_LOOKUP_VERSION_PARAM, Collections.singletonList(projectVersion))
                            );
                    return response.sendString(Mono.just("{\"name\":\"test-project\",\"uuid\":\"uuid-3\",\"version\":\"1.2.3\"}"));
                }))
                .bindNow();

        ApiClient uut = createClient();

        assertThat(uut.lookupProject(projectName, projectVersion)).isEqualTo(Project.builder().name(projectName).version(projectVersion).uuid("uuid-3").build());
    }

    @Test
    public void getFindingsTest() throws ApiClientException {
        server = HttpServer.create()
                .host("localhost")
                .port(0)
                .route(routes -> routes.get(ApiClient.PROJECT_FINDINGS_URL + "/{uuid}", (request, response) -> {
                    assertThat(request.requestHeaders().contains(ApiClient.API_KEY_HEADER, API_KEY, false)).isTrue();
                    assertThat(request.requestHeaders().contains(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON, true)).isTrue();
                    assertThat(request.param("uuid")).isNotEmpty();
                    String uuid = request.param("uuid");
                    switch (uuid) {
                        case "uuid-1":
                            return response.sendString(Mono.just("[]"));
                        default:
                            return response.sendNotFound();
                    }
                }))
                .bindNow();

        ApiClient uut = createClient();

        assertThatCode(() -> uut.getFindings("foo")).isInstanceOf(ApiClientException.class)
                .hasNoCause()
                .hasMessage(Messages.ApiClient_Error_RetrieveFindings(404, "Not Found"));

        assertThat(uut.getFindings("uuid-1")).isEmpty();
    }

    @Test
    public void uploadTestWithUuid() throws IOException, InterruptedException {
        File bom = tmpDir.newFile();
        Files.write(bom.toPath(), "<test />".getBytes(StandardCharsets.UTF_8));
        final AtomicReference<String> requestBody = new AtomicReference<>();
        final CountDownLatch completionSignal = new CountDownLatch(1);
        server = HttpServer.create()
                .host("localhost")
                .port(0)
                .route(routes -> routes.put(ApiClient.BOM_URL, (request, response) -> {
                    assertThat(request.requestHeaders().contains(ApiClient.API_KEY_HEADER, API_KEY, false)).isTrue();
                    assertThat(request.requestHeaders().contains(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON, true)).isTrue();
                    assertThat(request.requestHeaders().contains(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON, true)).isTrue();
                    assertThat(request.requestHeaders().contains(HttpHeaderNames.CONTENT_LENGTH)).isTrue();
                    return response.sendString(
                            request.receive().asString(StandardCharsets.UTF_8)
                            .doOnNext(body -> requestBody.set(body))
                            .doOnComplete(() -> completionSignal.countDown())
                            .map(body -> "{\"token\":\"uuid-1\"}")
                    );
                }))
                .bindNow();

        ApiClient uut = createClient();
        assertThat(uut.upload("uuid-1", null, null, new FilePath(bom), false)).isEqualTo(new UploadResult(true, "uuid-1"));
        
        File mockFile = mock(File.class);
        when(mockFile.getPath()).thenReturn(tmpDir.getRoot().getPath());
        FilePath fileWithError = new FilePath(mockFile);
        assertThat(uut.upload(null, "p1", "v1", fileWithError, true)).isEqualTo(new UploadResult(false));
        String expectedBody = "{\"bom\":\"PHRlc3QgLz4=\",\"project\":\"uuid-1\"}";
        completionSignal.await(5, TimeUnit.SECONDS);
        assertThat(requestBody.get()).isEqualTo(expectedBody);
    }

    @Test
    public void uploadTestWithName() throws IOException, InterruptedException {
        File bom = tmpDir.newFile();
        Files.write(bom.toPath(), "<test />".getBytes(StandardCharsets.UTF_8));
        final AtomicReference<String> requestBody = new AtomicReference<>();
        final CountDownLatch completionSignal = new CountDownLatch(1);
        server = HttpServer.create()
                .host("localhost")
                .port(0)
                .route(routes -> routes.put(ApiClient.BOM_URL, (request, response) -> {
                    assertThat(request.requestHeaders().contains(ApiClient.API_KEY_HEADER, API_KEY, false)).isTrue();
                    assertThat(request.requestHeaders().contains(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON, true)).isTrue();
                    assertThat(request.requestHeaders().contains(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON, true)).isTrue();
                    assertThat(request.requestHeaders().contains(HttpHeaderNames.CONTENT_LENGTH)).isTrue();
                    return response.sendString(
                            request.receive().asString(StandardCharsets.UTF_8)
                            .doOnNext(body -> requestBody.set(body))
                            .doOnComplete(() -> completionSignal.countDown())
                            .map(body -> "")
                    );
                }))
                .bindNow();

        ApiClient uut = createClient();
        assertThat(uut.upload(null, "p1", "v1", new FilePath(bom), false)).isEqualTo(new UploadResult(true));
        String expectedBody = "{\"bom\":\"PHRlc3QgLz4=\",\"projectName\":\"p1\",\"projectVersion\":\"v1\",\"autoCreate\":false}";
        completionSignal.await(5, TimeUnit.SECONDS);
        assertThat(requestBody.get()).isEqualTo(expectedBody);
    }

    @Test
    public void uploadTestWithErrors() throws IOException, InterruptedException {
        ApiClient uut;
        
        server = HttpServer.create().host("localhost").port(0).route(routes -> routes.put(ApiClient.BOM_URL, (request, response) -> response.status(HttpResponseStatus.BAD_REQUEST).send())).bindNow();
        uut = createClient();
        assertThat(uut.upload(null, "p1", "v1", new FilePath(tmpDir.newFile()), true)).isEqualTo(new UploadResult(false));
        verify(logger).log(Messages.Builder_Payload_Invalid());
        server.disposeNow();
        
        server = HttpServer.create().host("localhost").port(0).route(routes -> routes.put(ApiClient.BOM_URL, (request, response) -> response.status(HttpResponseStatus.UNAUTHORIZED).send())).bindNow();
        uut = createClient();
        assertThat(uut.upload(null, "p1", "v1", new FilePath(tmpDir.newFile()), true)).isEqualTo(new UploadResult(false));
        verify(logger).log(Messages.Builder_Unauthorized());
        server.disposeNow();
        
        server = HttpServer.create().host("localhost").port(0).route(routes -> routes.put(ApiClient.BOM_URL, (request, response) -> response.status(HttpResponseStatus.NOT_FOUND).send())).bindNow();
        uut = createClient();
        assertThat(uut.upload(null, "p1", "v1", new FilePath(tmpDir.newFile()), true)).isEqualTo(new UploadResult(false));
        verify(logger).log(Messages.Builder_Project_NotFound());
        server.disposeNow();

        File mockFile = mock(File.class);
        when(mockFile.getPath()).thenReturn(tmpDir.getRoot().getPath());
        FilePath fileWithError = new FilePath(mockFile);
        assertThat(uut.upload(null, "p1", "v1", fileWithError, true)).isEqualTo(new UploadResult(false));
        verify(logger).log(startsWith(Messages.Builder_Error_Processing(tmpDir.getRoot().getPath(), "")));
    }

    @Test
    public void isTokenBeingProcessedTest() throws ApiClientException {
        server = HttpServer.create()
                .host("localhost")
                .port(0)
                .route(routes -> routes.get(ApiClient.BOM_TOKEN_URL + "/{uuid}", (request, response) -> {
                    assertThat(request.requestHeaders().contains(ApiClient.API_KEY_HEADER, API_KEY, false)).isTrue();
                    assertThat(request.requestHeaders().contains(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON, true)).isTrue();
                    assertThat(request.param("uuid")).isNotEmpty();
                    String uuid = request.param("uuid");
                    switch (uuid) {
                        case "uuid-1":
                            return response.sendString(Mono.just("{\"processing\":true}"));
                        default:
                            return response.sendNotFound();
                    }
                }))
                .bindNow();

        ApiClient uut = createClient();

        assertThatCode(() -> uut.isTokenBeingProcessed("foo")).isInstanceOf(ApiClientException.class)
                .hasNoCause()
                .hasMessage(Messages.ApiClient_Error_TokenProcessing(404, "Not Found"));

        assertThat(uut.isTokenBeingProcessed("uuid-1")).isTrue();
    }
    
    @Test
    public void updateProjectPropertiesTest() throws ApiClientException, InterruptedException {
        final AtomicReference<String> requestBody = new AtomicReference<>();
        final CountDownLatch completionSignal = new CountDownLatch(1);
        server = HttpServer.create()
                .host("localhost")
                .port(0)
                .route(routes -> routes
                    .get(ApiClient.PROJECT_URL + "/uuid-3", (request, response) -> {
                        assertThat(request.requestHeaders().contains(ApiClient.API_KEY_HEADER, API_KEY, false)).isTrue();
                        assertThat(request.requestHeaders().contains(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON, true)).isTrue();
                        return response.sendString(Mono.just("{\"name\":\"test-project\",\"uuid\":\"uuid-3\",\"version\":\"1.2.3\",\"tags\":[{\"name\":\"tag1\"},{\"name\":\"tag2\"}],\"parent\":{\"uuid\":\"old-parent\"},\"parentUuid\":\"old-parent\"}"));
                    })
                    .post(ApiClient.PROJECT_URL, (request, response) -> {
                        assertThat(request.requestHeaders().contains(ApiClient.API_KEY_HEADER, API_KEY, false)).isTrue();
                        assertThat(request.requestHeaders().contains(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON, true)).isTrue();
                        return response.sendString(
                                request.receive().asString(StandardCharsets.UTF_8)
                                .doOnNext(body -> requestBody.set(body))
                                .doOnComplete(() -> completionSignal.countDown())
                        );
                    })
                )
                .bindNow();

        ApiClient uut = createClient();
        
        final ProjectProperties props = new ProjectProperties();
        props.setTags(Arrays.asList("tag2", "tag4"));
        props.setSwidTagId("my swid tag id");
        props.setGroup("my group");
        props.setDescription("my description");
        props.setParentId("parent-uuid");

        assertThatCode(() -> uut.updateProjectProperties("uuid-3", props)).doesNotThrowAnyException();
        completionSignal.await(5, TimeUnit.SECONDS);
        final JSONObject updatedProject = JSONObject.fromObject(requestBody.get());
        final Project project = ProjectParser.parse(updatedProject);
        assertThat(project.getTags()).containsExactlyInAnyOrder("tag1", "tag2", "tag4");
        assertThat(project.getSwidTagId()).isEqualTo(props.getSwidTagId());
        assertThat(project.getGroup()).isEqualTo(props.getGroup());
        assertThat(project.getDescription()).isEqualTo(props.getDescription());
        assertThat(project.getParent()).hasFieldOrPropertyWithValue("uuid", props.getParentId());
        assertThat(updatedProject.has("parentUuid")).isFalse();
    }

    @Test
    public void testGetTeamPermissions() throws ApiClientException {
        server = HttpServer.create()
                .host("localhost")
                .port(0)
                .route(routes -> routes.get(ApiClient.TEAM_SELF_URL, (request, response) -> {
                    return response.sendString(Mono.just("{\"name\":\"test-team\",\"permissions\":[{\"name\":\"BOM_UPLOAD\"},{\"name\":\"PROJECT_CREATION_UPLOAD\"}]}"));
                }))
                .bindNow();

        ApiClient uut = createClient();

        assertThat(uut.getTeamPermissions()).satisfies(team -> {
                assertThat(team.getName()).isEqualTo("test-team");
                assertThat(team.getPermissions()).containsExactlyInAnyOrder("BOM_UPLOAD", "PROJECT_CREATION_UPLOAD");
        });
    }

    @Test
    public void testGetVersion() throws ApiClientException {
        server = HttpServer.create()
                .host("localhost")
                .port(0)
                .route(routes -> routes.get(ApiClient.VERSION_URL, (request, response) -> {
                    return response.sendString(Mono.just("{\"version\":\"1.2.4\"}"));
                }))
                .bindNow();

        ApiClient uut = createClient();

        assertThat(uut.getVersion()).isEqualTo(new VersionNumber("1.2.4"));
    }

}
