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
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpClient;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import net.sf.json.JSONObject;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jenkinsci.plugins.DependencyTrack.model.Project;
import org.jenkinsci.plugins.DependencyTrack.model.UploadResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
@ExtendWith(MockitoExtension.class)
class ApiClientTest {

    private static final String API_KEY = "api-key";

    private DisposableServer server;

    @Mock
    private ConsoleLogger logger;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.disposeNow();
        }
    }

    private ApiClient createClient() {
        return new ApiClient(String.format("http://%s:%d", server.host(), server.port()), API_KEY, logger, 1, 1);
    }

    private ApiClient createClient(HttpClient httpClient) {
        return new ApiClient("http://host.tld", API_KEY, logger, 1, () -> httpClient);
    }

    @Test
    void testConnectionTest() throws ApiClientException {
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
    void testConnectionTestWithErrors() throws IOException, InterruptedException {
        final var httpClient = mock(HttpClient.class);
        final var uut = createClient(httpClient);
        doThrow(new ConnectException("oops"), new InterruptedException("oops"))
                .when(httpClient).send(any(), any());

        assertThatCode(() -> uut.testConnection())
                .hasMessage(Messages.ApiClient_Error_Connection("", ""))
                .hasCauseInstanceOf(ConnectException.class);

        assertThatCode(() -> uut.testConnection())
                .hasMessage(Messages.ApiClient_Error_Canceled())
                .hasCauseInstanceOf(InterruptedException.class);
        assertThat(Thread.currentThread().isInterrupted()).as("current Thread isInterrupted").isTrue();
    }

    @Test
    void testConnectionTestInternalError() {
        server = HttpServer.create()
                .host("localhost")
                .port(0)
                .route(routes -> routes.get(ApiClient.PROJECT_URL, (request, response) -> response.status(HttpResponseStatus.INTERNAL_SERVER_ERROR).sendString(Mono.just("something went wrong"))))
                .bindNow();

        ApiClient uut = createClient();

        assertThatCode(() -> uut.testConnection()).isInstanceOf(ApiClientException.class)
                .hasNoCause()
                .hasMessage(Messages.ApiClient_Error_Connection(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase()));

        verify(logger).log("something went wrong");
    }

    @Test()
    void getProjectsTest() throws ApiClientException {
        server = HttpServer.create()
                .host("localhost")
                .port(0)
                .route(routes -> routes.get(ApiClient.PROJECT_URL, (request, response) -> {
            assertThat(request.requestHeaders().contains(ApiClient.API_KEY_HEADER, API_KEY, false)).isTrue();
            assertThat(request.requestHeaders().contains(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON, true)).isTrue();
            QueryStringDecoder query = new QueryStringDecoder(request.uri());
            assertThat(query.parameters())
                    .contains(entry("limit", List.of("500")), entry("excludeInactive", List.of("true")))
                    .containsKey("page").extractingByKey("page", as(InstanceOfAssertFactories.list(String.class)))
                    .hasSize(1).first().satisfies(p -> {
                assertThat(Integer.valueOf(p)).isBetween(1, 3);
            });
            int page = Integer.parseInt(query.parameters().get("page").get(0));
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
                Project.builder().name("Project 1").uuid("uuid-1").tags(List.of()).build(),
                Project.builder().name("Project 2").uuid("uuid-2").version(isOldJsonVersion ? null : "null").tags(List.of()).build(),
                Project.builder().name("Project 3").uuid("uuid-3").version("1.2.3").tags(List.of("tag1", "tag2")).lastBomImport(LocalDateTime.of(2007, Month.DECEMBER, 3, 10, 15, 30)).build()
        );
    }

    @Test
    void getProjectsTestWithErrors() throws IOException, InterruptedException {
        final var httpClient = mock(HttpClient.class);
        final var uut = createClient(httpClient);
        doThrow(new ConnectException("oops"), new InterruptedException("oops"))
                .when(httpClient).send(any(), any());

        assertThatCode(() -> uut.getProjects())
                .hasMessage(Messages.ApiClient_Error_Connection("", ""))
                .hasCauseInstanceOf(ConnectException.class);

        assertThatCode(() -> uut.getProjects())
                .hasMessage(Messages.ApiClient_Error_Canceled())
                .hasCauseInstanceOf(InterruptedException.class);
        assertThat(Thread.currentThread().isInterrupted()).as("current Thread isInterrupted").isTrue();
    }

    @Test
    void lookupProjectTest() throws ApiClientException {
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
                            entry(ApiClient.PROJECT_LOOKUP_NAME_PARAM, List.of(projectName)),
                            entry(ApiClient.PROJECT_LOOKUP_VERSION_PARAM, List.of(projectVersion))
                    );
            return response.sendString(Mono.just("{\"name\":\"test-project\",\"uuid\":\"uuid-3\",\"version\":\"1.2.3\"}"));
        }))
                .bindNow();

        ApiClient uut = createClient();

        assertThat(uut.lookupProject(projectName, projectVersion)).isEqualTo(Project.builder().name(projectName).version(projectVersion).uuid("uuid-3").build());

        server.disposeNow();
        server = HttpServer.create().host("localhost").port(0).route(routes -> routes.get(ApiClient.PROJECT_LOOKUP_URL, (request, response) -> response.status(HttpResponseStatus.NOT_FOUND).send())).bindNow();
        assertThatCode(() -> createClient().lookupProject(projectName, projectVersion))
                .hasMessage(Messages.ApiClient_Error_ProjectLookup(projectName, projectVersion, HttpResponseStatus.NOT_FOUND.code(), HttpResponseStatus.NOT_FOUND.reasonPhrase()))
                .hasNoCause();
        verify(logger).log("");
    }

    @Test
    void lookupProjectTestWithErrors() throws IOException, InterruptedException {
        final var httpClient = mock(HttpClient.class);
        final var uut = createClient(httpClient);
        doThrow(new ConnectException("oops"), new InterruptedException("oops"))
                .when(httpClient).send(any(), any());

        assertThatCode(() -> uut.lookupProject("", ""))
                .hasMessage(Messages.ApiClient_Error_Connection("", ""))
                .hasCauseInstanceOf(ConnectException.class);

        assertThatCode(() -> uut.lookupProject("", ""))
                .hasMessage(Messages.ApiClient_Error_Canceled())
                .hasCauseInstanceOf(InterruptedException.class);
        assertThat(Thread.currentThread().isInterrupted()).as("current Thread isInterrupted").isTrue();
    }

    @Test
    void getFindingsTest() throws ApiClientException {
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
                .hasMessage(Messages.ApiClient_Error_RetrieveFindings(HttpResponseStatus.NOT_FOUND.code(), HttpResponseStatus.NOT_FOUND.reasonPhrase()));

        assertThat(uut.getFindings("uuid-1")).isEmpty();
    }

    @Test
    void getFindingsTestWithErrors() throws IOException, InterruptedException {
        final var httpClient = mock(HttpClient.class);
        final var uut = createClient(httpClient);
        doThrow(new ConnectException("oops"), new InterruptedException("oops"))
                .when(httpClient).send(any(), any());

        assertThatCode(() -> uut.getFindings("foo"))
                .hasMessage(Messages.ApiClient_Error_Connection("", ""))
                .hasCauseInstanceOf(ConnectException.class);

        assertThatCode(() -> uut.getFindings("foo"))
                .hasMessage(Messages.ApiClient_Error_Canceled())
                .hasCauseInstanceOf(InterruptedException.class);
        assertThat(Thread.currentThread().isInterrupted()).as("current Thread isInterrupted").isTrue();
    }

    @Test
    void uploadTestWithUuid(@TempDir Path tmp) throws IOException, InterruptedException {
        Path bom = tmp.resolve("bom.xml");
        Files.writeString(bom, "<test />", Charset.defaultCharset());
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
        assertThat(uut.upload("uuid-1", null, null, new FilePath(bom.toFile()), false)).isEqualTo(new UploadResult(true, "uuid-1"));

        File mockFile = mock(File.class);
        when(mockFile.getPath()).thenReturn(tmp.toAbsolutePath().toString());
        FilePath fileWithError = new FilePath(mockFile);
        assertThat(uut.upload(null, "p1", "v1", fileWithError, true)).isEqualTo(new UploadResult(false));
        String expectedBody = "{\"bom\":\"PHRlc3QgLz4=\",\"project\":\"uuid-1\"}";
        completionSignal.await(5, TimeUnit.SECONDS);
        assertThat(requestBody.get()).isEqualTo(expectedBody);
    }

    @Test
    void uploadTestWithName(@TempDir Path tmp) throws IOException, InterruptedException {
        Path bom = tmp.resolve("bom.xml");
        Files.writeString(bom, "<test />", Charset.defaultCharset());
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
        assertThat(uut.upload(null, "p1", "v1", new FilePath(bom.toFile()), false)).isEqualTo(new UploadResult(true));
        String expectedBody = "{\"bom\":\"PHRlc3QgLz4=\",\"projectName\":\"p1\",\"projectVersion\":\"v1\",\"autoCreate\":false}";
        completionSignal.await(5, TimeUnit.SECONDS);
        assertThat(requestBody.get()).isEqualTo(expectedBody);
    }

    @Test
    void uploadTestWithErrors(@TempDir Path tmp) throws IOException, InterruptedException {
        ApiClient uut;
        File bom = tmp.resolve("bom.xml").toFile();
        bom.createNewFile();

        server = HttpServer.create().host("localhost").port(0).route(routes -> routes.put(ApiClient.BOM_URL, (request, response) -> response.status(HttpResponseStatus.BAD_REQUEST).send())).bindNow();
        uut = createClient();
        assertThat(uut.upload(null, "p1", "v1", new FilePath(bom), true)).isEqualTo(new UploadResult(false));
        verify(logger).log(Messages.Builder_Payload_Invalid());
        server.disposeNow();

        server = HttpServer.create().host("localhost").port(0).route(routes -> routes.put(ApiClient.BOM_URL, (request, response) -> response.status(HttpResponseStatus.UNAUTHORIZED).send())).bindNow();
        uut = createClient();
        assertThat(uut.upload(null, "p1", "v1", new FilePath(bom), true)).isEqualTo(new UploadResult(false));
        verify(logger).log(Messages.Builder_Unauthorized());
        server.disposeNow();

        server = HttpServer.create().host("localhost").port(0).route(routes -> routes.put(ApiClient.BOM_URL, (request, response) -> response.status(HttpResponseStatus.NOT_FOUND).send())).bindNow();
        uut = createClient();
        assertThat(uut.upload(null, "p1", "v1", new FilePath(bom), true)).isEqualTo(new UploadResult(false));
        verify(logger).log(Messages.Builder_Project_NotFound());
        server.disposeNow();

        server = HttpServer.create().host("localhost").port(0).route(routes -> routes.put(ApiClient.BOM_URL, (request, response) -> response.status(HttpResponseStatus.GONE).send())).bindNow();
        uut = createClient();
        assertThat(uut.upload(null, "p1", "v1", new FilePath(bom), true)).isEqualTo(new UploadResult(false));
        verify(logger).log(Messages.ApiClient_Error_Connection(HttpResponseStatus.GONE.code(), HttpResponseStatus.GONE.reasonPhrase()));
        server.disposeNow();

        File mockFile = mock(File.class);
        when(mockFile.getPath()).thenReturn(tmp.toAbsolutePath().toString());
        FilePath fileWithError = new FilePath(mockFile);
        assertThat(uut.upload(null, "p1", "v1", fileWithError, true)).isEqualTo(new UploadResult(false));
        verify(logger).log(startsWith(Messages.Builder_Error_Processing(tmp.toAbsolutePath().toString(), "")));
    }

    @Test
    void isTokenBeingProcessedTest() throws ApiClientException {
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
                .hasMessage(Messages.ApiClient_Error_TokenProcessing(HttpResponseStatus.NOT_FOUND.code(), HttpResponseStatus.NOT_FOUND.reasonPhrase()));
        verify(logger).log("");

        assertThat(uut.isTokenBeingProcessed("uuid-1")).isTrue();
    }

    @Test
    void isTokenBeingProcessedTestWithErrors() throws IOException, InterruptedException {
        final var httpClient = mock(HttpClient.class);
        final var uut = createClient(httpClient);
        doThrow(new ConnectException("oops"), new InterruptedException("oops"))
                .when(httpClient).send(any(), any());

        assertThatCode(() -> uut.isTokenBeingProcessed("foo"))
                .hasMessage(Messages.ApiClient_Error_Connection("", ""))
                .hasCauseInstanceOf(ConnectException.class);

        assertThatCode(() -> uut.isTokenBeingProcessed("foo"))
                .hasMessage(Messages.ApiClient_Error_Canceled())
                .hasCauseInstanceOf(InterruptedException.class);
        assertThat(Thread.currentThread().isInterrupted()).as("current Thread isInterrupted").isTrue();
    }

    @Test
    void updateProjectPropertiesTest() throws ApiClientException, InterruptedException {
        final AtomicReference<String> requestBody = new AtomicReference<>();
        final CountDownLatch completionSignal = new CountDownLatch(1);
        server = HttpServer.create()
                .host("localhost")
                .port(0)
                .route(routes -> routes
                .route(request -> request.uri().equals(ApiClient.PROJECT_URL + "/uuid-3") && request.method().equals(HttpMethod.PATCH), (request, response) -> {
                    assertThat(request.requestHeaders().contains(ApiClient.API_KEY_HEADER, API_KEY, false)).isTrue();
                    assertThat(request.requestHeaders().contains(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON, true)).isTrue();
                    assertThat(request.requestHeaders().contains(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON, true)).isTrue();
                    assertThat(request.requestHeaders().contains(HttpHeaderNames.CONTENT_LENGTH)).isTrue();
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
        props.setTags(List.of("tag2", "tag4"));
        props.setSwidTagId("my swid tag id");
        props.setGroup("my group");
        props.setDescription("my description");
        props.setParentId("parent-uuid");

        assertThatCode(() -> uut.updateProjectProperties("uuid-3", props)).doesNotThrowAnyException();
        completionSignal.await(5, TimeUnit.SECONDS);
        final JSONObject updatedProject = JSONObject.fromObject(requestBody.get());
        final Project project = ProjectParser.parse(updatedProject);
        assertThat(project.getTags()).containsExactlyInAnyOrderElementsOf(props.getTags());
        assertThat(project.getSwidTagId()).isEqualTo(props.getSwidTagId());
        assertThat(project.getGroup()).isEqualTo(props.getGroup());
        assertThat(project.getDescription()).isEqualTo(props.getDescription());
        assertThat(project.getParent()).hasFieldOrPropertyWithValue("uuid", props.getParentId());
        assertThat(updatedProject.has("parentUuid")).isFalse();

        assertThatCode(() -> createClient().updateProjectProperties("uuid-unknown", new ProjectProperties()))
                .hasMessage(Messages.ApiClient_Error_ProjectUpdate("uuid-unknown", HttpResponseStatus.NOT_FOUND.code(), HttpResponseStatus.NOT_FOUND.reasonPhrase()))
                .hasNoCause();
        verify(logger).log("");
    }

    @Test
    void updateProjectPropertiesTestWithErrorsOnUpdate() throws IOException, InterruptedException {
        final var httpClient = mock(HttpClient.class);
        final var uut = createClient(httpClient);
        doThrow(new ConnectException("oops"), new InterruptedException("oops"))
                .when(httpClient).send(any(), any());

        assertThatCode(() -> uut.updateProjectProperties("foo", new ProjectProperties()))
                .hasMessage(Messages.ApiClient_Error_Connection("", ""))
                .hasCauseInstanceOf(ConnectException.class);

        assertThatCode(() -> uut.updateProjectProperties("foo", new ProjectProperties()))
                .hasMessage(Messages.ApiClient_Error_Canceled())
                .hasCauseInstanceOf(InterruptedException.class);
        assertThat(Thread.currentThread().isInterrupted()).as("current Thread isInterrupted").isTrue();
    }

    @Test
    void getTeamPermissionsTest() throws ApiClientException {
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

        server.disposeNow();
        server = HttpServer.create().host("localhost").port(0).route(routes -> routes.get(ApiClient.TEAM_SELF_URL, (request, response) -> response.status(HttpResponseStatus.NOT_FOUND).send())).bindNow();
        assertThatCode(() -> createClient().getTeamPermissions())
                .hasMessage(Messages.ApiClient_Error_Connection(HttpResponseStatus.NOT_FOUND.code(), HttpResponseStatus.NOT_FOUND.reasonPhrase()))
                .hasNoCause();
        verify(logger).log("");
    }

    @Test
    void getTeamPermissionsTestWithErrors() throws IOException, InterruptedException {
        final var httpClient = mock(HttpClient.class);
        final var uut = createClient(httpClient);
        doThrow(new ConnectException("oops"), new InterruptedException("oops"))
                .when(httpClient).send(any(), any());

        assertThatCode(() -> uut.getTeamPermissions())
                .hasMessage(Messages.ApiClient_Error_Connection("", ""))
                .hasCauseInstanceOf(ConnectException.class);

        assertThatCode(() -> uut.getTeamPermissions())
                .hasMessage(Messages.ApiClient_Error_Canceled())
                .hasCauseInstanceOf(InterruptedException.class);
        assertThat(Thread.currentThread().isInterrupted()).as("current Thread isInterrupted").isTrue();
    }

    @Test
    void testGetVersion() throws ApiClientException {
        server = HttpServer.create()
                .host("localhost")
                .port(0)
                .route(routes -> routes.get(ApiClient.VERSION_URL, (request, response) -> {
            return response.sendString(Mono.just("{\"version\":\"1.2.4\"}"));
        }))
                .bindNow();

        ApiClient uut = createClient();

        assertThat(uut.getVersion()).isEqualTo(new VersionNumber("1.2.4"));

        server.disposeNow();
        server = HttpServer.create().host("localhost").port(0).route(routes -> routes.get(ApiClient.VERSION_URL, (request, response) -> response.status(HttpResponseStatus.BAD_REQUEST).send())).bindNow();
        assertThatCode(() -> createClient().getVersion())
                .hasMessage(Messages.ApiClient_Error_Connection(HttpResponseStatus.BAD_REQUEST.code(), HttpResponseStatus.BAD_REQUEST.reasonPhrase()))
                .hasNoCause();
        verify(logger).log("");
    }

    @Test
    void testGetVersionWithErrors() throws IOException, InterruptedException {
        final var httpClient = mock(HttpClient.class);
        final var uut = createClient(httpClient);
        doThrow(new ConnectException("oops"), new InterruptedException("oops"))
                .when(httpClient).send(any(), any());

        assertThatCode(() -> uut.getVersion())
                .hasMessage(Messages.ApiClient_Error_Connection("", ""))
                .hasCauseInstanceOf(ConnectException.class);

        assertThatCode(() -> uut.getVersion())
                .hasMessage(Messages.ApiClient_Error_Canceled())
                .hasCauseInstanceOf(InterruptedException.class);
        assertThat(Thread.currentThread().isInterrupted()).as("current Thread isInterrupted").isTrue();
    }

}
