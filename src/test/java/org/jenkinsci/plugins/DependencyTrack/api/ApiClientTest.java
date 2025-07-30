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
package org.jenkinsci.plugins.DependencyTrack.api;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.HttpData;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import net.sf.json.JSONObject;
import okhttp3.OkHttpClient;
import org.apache.commons.io.function.Uncheck;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jenkinsci.plugins.DependencyTrack.model.Project;
import org.jenkinsci.plugins.DependencyTrack.model.ProjectParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
@ExtendWith(MockitoExtension.class)
@WithJenkins
class ApiClientTest {

    private static final String API_KEY = "api-key";

    private DisposableServer server;

    @Mock
    private Logger logger;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.disposeNow();
        }
    }

    private ApiClient createClient() {
        return new ApiClient(String.format("http://%s:%d", server.host(), server.port()), API_KEY, logger, new OkHttpClient());
    }

    private ApiClient createClient(OkHttpClient httpClient) {
        return new ApiClient("http://host.tld", API_KEY, logger, httpClient);
    }

    private void assertCommonHeaders(HttpServerRequest request) {
        assertThat(request.requestHeaders().contains(ApiClient.API_KEY_HEADER, API_KEY, false))
                .describedAs("Header '%s' must have value '%s'", ApiClient.API_KEY_HEADER, API_KEY)
                .isTrue();
        assertThat(request.requestHeaders().contains(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON, true))
                .describedAs("Header '%s' must have value '%s'", HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON)
                .isTrue();
    }

    private void assertPOSTHeaders(HttpServerRequest request) {
        assertThat(request.requestHeaders().get(HttpHeaderNames.CONTENT_TYPE))
                .describedAs("Header '%s' must start with value '%s'", HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                .startsWithIgnoringCase(HttpHeaderValues.APPLICATION_JSON);
        assertThat(request.requestHeaders().getInt(HttpHeaderNames.CONTENT_LENGTH))
                .describedAs("Header '%s' must be present with value greater than zero", HttpHeaderNames.CONTENT_LENGTH)
                .isPositive();
    }

    @Test
    void testConnectionTest(JenkinsRule r) throws ApiClientException {
        server = HttpServer.create()
                // no ipv6 due to https://bugs.openjdk.java.net/browse/JDK-8220663
                .host("localhost")
                .port(0)
                .route(routes -> routes.get(ApiClient.PROJECT_URL, (request, response) -> {
            assertCommonHeaders(request);
            return response.addHeader("X-Powered-By", "Dependency-Track v3.8.0").send();
        }))
                .bindNow();

        ApiClient uut = createClient();

        assertThat(uut.testConnection()).isEqualTo("Dependency-Track v3.8.0");
    }

    @Test
    void testConnectionTestWithErrors() throws IOException {
        final var httpClient = mock(OkHttpClient.class);
        final var call = mock(okhttp3.Call.class);
        final var uut = createClient(httpClient);
        when(httpClient.newCall(any(okhttp3.Request.class))).thenReturn(call);
        doThrow(new ConnectException("oops"))
                .when(call).execute();

        assertThatCode(uut::testConnection)
                .hasMessage(Messages.ApiClient_Error_Connection("", ""))
                .hasCauseInstanceOf(ConnectException.class);
        verify(httpClient, times(2)).newCall(any(okhttp3.Request.class));
    }

    @Test
    void testConnectionTestInternalError(JenkinsRule r) {
        server = HttpServer.create()
                .host("localhost")
                .port(0)
                .route(routes -> routes.get(ApiClient.PROJECT_URL, (request, response) -> response.status(HttpResponseStatus.INTERNAL_SERVER_ERROR).sendString(Mono.just("something went wrong"))))
                .bindNow();

        ApiClient uut = createClient();

        assertThatCode(uut::testConnection).isInstanceOf(ApiClientException.class)
                .hasNoCause()
                .hasMessage(Messages.ApiClient_Error_Connection(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase()));

        verify(logger).log("something went wrong");
    }

    @Test
    void getProjectsTest(JenkinsRule r) throws ApiClientException {
        server = HttpServer.create()
                .host("localhost")
                .port(0)
                .route(routes -> routes.get(ApiClient.PROJECT_URL, (request, response) -> {
            assertCommonHeaders(request);
            QueryStringDecoder query = new QueryStringDecoder(request.uri());
            assertThat(query.parameters())
                    .containsOnlyKeys(ApiClient.PAGINATED_REQ_PAGE_PARAM, "excludeInactive", ApiClient.PAGINATED_REQ_PAGESIZE_PARAM, "sortName", "sortOrder")
                    .containsEntry(ApiClient.PAGINATED_REQ_PAGESIZE_PARAM, List.of("500"))
                    .containsEntry("sortName", List.of("name"))
                    .containsEntry("sortOrder", List.of("asc"))
                    .containsEntry("excludeInactive", List.of("true"))
                    .extractingByKey(ApiClient.PAGINATED_REQ_PAGE_PARAM, as(InstanceOfAssertFactories.list(String.class)))
                    .hasSize(1).first().satisfies(p -> {
                assertThat(Integer.valueOf(p)).isBetween(1, 2);
            });
            int page = Integer.parseInt(query.parameters().get(ApiClient.PAGINATED_REQ_PAGE_PARAM).get(0));
            int totalCount = 3;
            return switch (page) {
                case 1 ->
                    response
                    .header(ApiClient.PAGINATED_RES_TOTAL_COUNT_HEADER, String.valueOf(totalCount))
                    .sendString(Mono.just("[{\"name\":\"Project 1\",\"uuid\":\"uuid-1\",\"version\":null},{\"name\":\"Project 2\",\"uuid\":\"uuid-2\",\"version\":\"null\"}]"));
                case 2 ->
                    response
                    .header(ApiClient.PAGINATED_RES_TOTAL_COUNT_HEADER, String.valueOf(totalCount))
                    .sendString(Mono.just("[{\"name\":\"Project 3\",\"uuid\":\"uuid-3\",\"version\":\"1.2.3\",\"lastBomImportStr\":\"2007-12-03T10:15:30\",\"tags\":[{\"name\":\"tag1\"},{\"name\":\"tag2\"},{\"name\":null}]}]"));
                default ->
                    response.sendString(Mono.just("[]"));
            };
        }))
                .bindNow();

        ApiClient uut = createClient();
        final List<Project> projects = uut.getProjects();

        assertThat(projects).containsExactly(
                Project.builder().name("Project 1").uuid("uuid-1").tags(List.of()).build(),
                Project.builder().name("Project 2").uuid("uuid-2").version("null").tags(List.of()).build(),
                Project.builder().name("Project 3").uuid("uuid-3").version("1.2.3").tags(List.of("tag1", "tag2")).lastBomImport(LocalDateTime.of(2007, Month.DECEMBER, 3, 10, 15, 30)).build()
        );
    }

    @Test
    void getProjectsTestWithErrors() throws IOException {
        final var httpClient = mock(OkHttpClient.class);
        final var call = mock(okhttp3.Call.class);
        final var uut = createClient(httpClient);
        when(httpClient.newCall(any(okhttp3.Request.class))).thenReturn(call);
        doThrow(new ConnectException("oops"))
                .when(call).execute();

        assertThatCode(uut::getProjects)
                .hasMessage(Messages.ApiClient_Error_Connection("", ""))
                .hasCauseInstanceOf(ConnectException.class);
        verify(httpClient, times(2)).newCall(any(okhttp3.Request.class));
    }

    @Test
    void lookupProjectTest(JenkinsRule r) throws ApiClientException {
        String projectName = "test-project";
        String projectVersion = "1.2.3";
        server = HttpServer.create()
                .host("localhost")
                .port(0)
                .route(routes -> routes.get(ApiClient.PROJECT_LOOKUP_URL, (request, response) -> {
            assertCommonHeaders(request);
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
    void lookupProjectTestWithErrors() throws IOException {
        final var httpClient = mock(OkHttpClient.class);
        final var call = mock(okhttp3.Call.class);
        final var uut = createClient(httpClient);
        when(httpClient.newCall(any(okhttp3.Request.class))).thenReturn(call);
        doThrow(new ConnectException("oops"))
                .when(call).execute();

        assertThatCode(() -> uut.lookupProject("", ""))
                .hasMessage(Messages.ApiClient_Error_Connection("", ""))
                .hasCauseInstanceOf(ConnectException.class);
        verify(httpClient, times(2)).newCall(any(okhttp3.Request.class));
    }

    @Test
    void getFindingsTest(JenkinsRule r) throws ApiClientException {
        server = HttpServer.create()
                .host("localhost")
                .port(0)
                .route(routes -> routes.get(ApiClient.PROJECT_FINDINGS_URL + "/{uuid}", (request, response) -> {
            assertCommonHeaders(request);
            assertThat(request.param("uuid")).isNotEmpty();
            String uuid = request.param("uuid");
            return switch (uuid) {
                case "uuid-1" ->
                    response.sendString(Mono.just("[]"));
                default ->
                    response.sendNotFound();
            };
        }))
                .bindNow();

        ApiClient uut = createClient();

        assertThatCode(() -> uut.getFindings("foo")).isInstanceOf(ApiClientException.class)
                .hasNoCause()
                .hasMessage(Messages.ApiClient_Error_RetrieveFindings(HttpResponseStatus.NOT_FOUND.code(), HttpResponseStatus.NOT_FOUND.reasonPhrase()));

        assertThat(uut.getFindings("uuid-1")).isEmpty();
    }

    @Test
    void getFindingsTestWithErrors() throws IOException {
        final var httpClient = mock(OkHttpClient.class);
        final var call = mock(okhttp3.Call.class);
        final var uut = createClient(httpClient);
        when(httpClient.newCall(any(okhttp3.Request.class))).thenReturn(call);
        doThrow(new ConnectException("oops"))
                .when(call).execute();

        assertThatCode(() -> uut.getFindings("foo"))
                .hasMessage(Messages.ApiClient_Error_Connection("", ""))
                .hasCauseInstanceOf(ConnectException.class);
        verify(httpClient, times(2)).newCall(any(okhttp3.Request.class));
    }

    @Test
    void getViolationsTest(JenkinsRule r) throws ApiClientException {
        server = HttpServer.create()
                .host("localhost")
                .port(0)
                .route(routes -> routes.get(ApiClient.PROJECT_VIOLATIONS_URL + "/{uuid}", (request, response) -> {
            assertCommonHeaders(request);
            QueryStringDecoder query = new QueryStringDecoder(request.uri());
            assertThat(query.parameters())
                    .containsOnlyKeys(ApiClient.PAGINATED_REQ_PAGE_PARAM, ApiClient.PAGINATED_REQ_PAGESIZE_PARAM)
                    .containsEntry(ApiClient.PAGINATED_REQ_PAGESIZE_PARAM, List.of("100"))
                    .extractingByKey(ApiClient.PAGINATED_REQ_PAGE_PARAM, as(InstanceOfAssertFactories.list(String.class)))
                    .hasSize(1).first().satisfies(p -> {
                assertThat(Integer.valueOf(p)).isBetween(1, 2);
            });
            String projectId = request.param("uuid");
            assertThat(projectId).isNotEmpty();
            return switch (projectId) {
                case "uuid-1" -> {
                    int page = Integer.parseInt(query.parameters().get(ApiClient.PAGINATED_REQ_PAGE_PARAM).get(0));
                    int totalCount = 2;
                    yield switch (page) {
                        case 1 ->
                            response
                            .header(ApiClient.PAGINATED_RES_TOTAL_COUNT_HEADER, String.valueOf(totalCount))
                            .sendString(Mono.just("[{\"type\":\"SECURITY\",\"component\":{\"uuid\":\"component-1\",\"name\":\"name-2\",\"group\":\"group-2\",\"version\":\"version-2\",\"purl\":\"purl-2\"},\"policyCondition\":{\"policy\":{\"name\":\"my-rule1\",\"violationState\":\"INFO\"}},\"uuid\":\"violation-1\"}]"));
                        case 2 ->
                            response
                            .header(ApiClient.PAGINATED_RES_TOTAL_COUNT_HEADER, String.valueOf(totalCount))
                            .sendString(Mono.just("[{\"type\":\"SECURITY\",\"component\":{\"uuid\":\"component-2\",\"name\":\"name-2\",\"group\":\"group-2\",\"version\":\"version-2\",\"purl\":\"purl-2\"},\"policyCondition\":{\"policy\":{\"name\":\"my-rule2\",\"violationState\":\"INFO\"}},\"uuid\":\"violation-2\"}]"));
                        default ->
                            response.sendString(Mono.just("[]"));
                    };
                }
                default ->
                    response.sendNotFound();
            };
        }))
                .bindNow();

        ApiClient uut = createClient();

        assertThatCode(() -> uut.getViolations("foo")).isInstanceOf(ApiClientException.class)
                .hasNoCause()
                .hasMessage(Messages.ApiClient_Error_RetrieveViolations(HttpResponseStatus.NOT_FOUND.code(), HttpResponseStatus.NOT_FOUND.reasonPhrase()));

        assertThat(uut.getViolations("uuid-1")).hasSize(2);
    }

    @Test
    void getViolationsTestWithErrors() throws IOException {
        final var httpClient = mock(OkHttpClient.class);
        final var call = mock(okhttp3.Call.class);
        final var uut = createClient(httpClient);
        when(httpClient.newCall(any(okhttp3.Request.class))).thenReturn(call);
        doThrow(new ConnectException("oops"))
                .when(call).execute();

        assertThatCode(() -> uut.getViolations("foo"))
                .hasMessage(Messages.ApiClient_Error_Connection("", ""))
                .hasCauseInstanceOf(ConnectException.class);
        verify(httpClient, times(2)).newCall(any(okhttp3.Request.class));
    }

    @Test
    void uploadTestWithUuid(@TempDir Path tmp, JenkinsRule r) throws IOException, InterruptedException {
        final AtomicReference<Map<String, String>> requestBody = new AtomicReference<>();
        final CountDownLatch completionSignal = new CountDownLatch(1);
        server = HttpServer.create()
                .host("localhost")
                .port(0)
                .route(routes -> routes.post(ApiClient.BOM_URL, (request, response) -> {
            assertCommonHeaders(request);
            assertThat(request.isMultipart()).isTrue();
            return response.sendString(request.receiveForm()
                    .collectMap(HttpData::getName, v -> Uncheck.apply(HttpData::getString, v))
                    .doOnSuccess(m -> {
                        requestBody.set(m);
                        completionSignal.countDown();
                    })
                    .map(body -> "{\"token\":\"uuid-1\"}"));
        })).bindNow();

        final var props = new ProjectData.Properties(null, null, null, null, "parent-uuid", null, null, true);

        ApiClient uut = createClient();
        var data = new ProjectData("uuid-1", null, null, false, props);
        assertThat(uut.upload(data, "<test />")).isEqualTo(new UploadResult(true, "uuid-1"));

        completionSignal.await(5, TimeUnit.SECONDS);
        assertThat(completionSignal.getCount()).isZero();
        assertThat(requestBody).hasValueSatisfying(body -> assertThat(body).containsOnly(entry("bom", "<test />"), entry("project", "uuid-1"), entry("parentUUID", "parent-uuid"), entry("isLatest", "true")));
    }

    @Test
    void uploadTestWithName(@TempDir Path tmp, JenkinsRule r) throws IOException, InterruptedException {
        final AtomicReference<Map<String, String>> requestBody = new AtomicReference<>();
        final CountDownLatch completionSignal = new CountDownLatch(1);
        server = HttpServer.create()
                .host("localhost")
                .port(0)
                .route(routes -> routes.post(ApiClient.BOM_URL, (request, response) -> {
            assertCommonHeaders(request);
            assertThat(request.isMultipart()).isTrue();
           return response.sendString(request.receiveForm()
                    .collectMap(HttpData::getName, v -> Uncheck.apply(HttpData::getString, v))
                    .doOnSuccess(m -> {
                        requestBody.set(m);
                        completionSignal.countDown();
                    })
                    .map(body -> ""));
        })).bindNow();

        final var props = new ProjectData.Properties(null, null, null, null, null, "parent-name", "parent-version", null);

        ApiClient uut = createClient();
        var data = new ProjectData(null, "p1", "v1", false, props);
        assertThat(uut.upload(data, "<test />")).isEqualTo(new UploadResult(true));

        completionSignal.await(5, TimeUnit.SECONDS);
        assertThat(completionSignal.getCount()).isZero();
        assertThat(requestBody).hasValueSatisfying(body -> assertThat(body).containsOnly(entry("bom", "<test />"), entry("projectName", "p1"), entry("projectVersion", "v1"), entry("autoCreate", "false"), entry("parentName", "parent-name"), entry("parentVersion", "parent-version")));
    }

    @Test
    void uploadTestWithErrors(JenkinsRule r) throws IOException {
        ApiClient uut;
        var data = new ProjectData(null, "p1", "v1", true, null);

        server = HttpServer.create().host("localhost").port(0).route(routes -> routes.post(ApiClient.BOM_URL, (request, response) -> response.status(HttpResponseStatus.BAD_REQUEST).send())).bindNow();
        uut = createClient();
        assertThat(uut.upload(data, "")).isEqualTo(new UploadResult(false));
        verify(logger).log(Messages.ApiClient_Payload_Invalid());
        server.disposeNow();

        server = HttpServer.create().host("localhost").port(0).route(routes -> routes.post(ApiClient.BOM_URL, (request, response) -> response.status(HttpResponseStatus.UNAUTHORIZED).send())).bindNow();
        uut = createClient();
        assertThat(uut.upload(data, "")).isEqualTo(new UploadResult(false));
        verify(logger).log(Messages.ApiClient_Unauthorized());
        server.disposeNow();

        server = HttpServer.create().host("localhost").port(0).route(routes -> routes.post(ApiClient.BOM_URL, (request, response) -> response.status(HttpResponseStatus.NOT_FOUND).send())).bindNow();
        uut = createClient();
        assertThat(uut.upload(data, "")).isEqualTo(new UploadResult(false));
        verify(logger).log(Messages.ApiClient_Project_NotFound());
        server.disposeNow();

        server = HttpServer.create().host("localhost").port(0).route(routes -> routes.post(ApiClient.BOM_URL, (request, response) -> response.status(HttpResponseStatus.GONE).send())).bindNow();
        uut = createClient();
        assertThat(uut.upload(data, "")).isEqualTo(new UploadResult(false));
        verify(logger).log(Messages.ApiClient_Error_Connection(HttpResponseStatus.GONE.code(), HttpResponseStatus.GONE.reasonPhrase()));
        server.disposeNow();
        
        final var httpClient = mock(OkHttpClient.class);
        final var call = mock(okhttp3.Call.class);
        final var uutWithMock = createClient(httpClient);
        when(httpClient.newCall(any(okhttp3.Request.class))).thenReturn(call);
        doThrow(new java.net.SocketTimeoutException("oops"))
                .when(call).execute();

        assertThatCode(() -> uutWithMock.upload(data, ""))
                .hasMessage(Messages.ApiClient_Error_Connection("", ""))
                .hasCauseInstanceOf(java.net.SocketTimeoutException.class);
        verify(httpClient, times(2)).newCall(any(okhttp3.Request.class));
    }

    @Test
    void isTokenBeingProcessedTest(JenkinsRule r) throws ApiClientException {
        server = HttpServer.create()
                .host("localhost")
                .port(0)
                .route(routes -> routes.get(ApiClient.BOM_TOKEN_URL + "/{uuid}", (request, response) -> {
            assertCommonHeaders(request);
            assertThat(request.param("uuid")).isNotEmpty();
            String uuid = request.param("uuid");
            return switch (uuid) {
                case "uuid-1" ->
                    response.sendString(Mono.just("{\"processing\":true}"));
                default ->
                    response.sendNotFound();
            };
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
    void isTokenBeingProcessedTestWithErrors() throws IOException {
        final var httpClient = mock(OkHttpClient.class);
        final var call = mock(okhttp3.Call.class);
        final var uut = createClient(httpClient);
        when(httpClient.newCall(any(okhttp3.Request.class))).thenReturn(call);
        doThrow(new ConnectException("oops"))
                .when(call).execute();

        assertThatCode(() -> uut.isTokenBeingProcessed("foo"))
                .hasMessage(Messages.ApiClient_Error_Connection("", ""))
                .hasCauseInstanceOf(ConnectException.class);
        verify(httpClient, times(2)).newCall(any(okhttp3.Request.class));
    }

    @Test
    void updateProjectPropertiesTest(JenkinsRule r) throws InterruptedException {
        final AtomicReference<String> requestBody = new AtomicReference<>();
        final CountDownLatch completionSignal = new CountDownLatch(1);
        server = HttpServer.create()
                .host("localhost")
                .port(0)
                .route(routes -> routes
                .route(request -> request.uri().equals(ApiClient.PROJECT_URL + "/uuid-3") && request.method().equals(HttpMethod.PATCH), (request, response) -> {
                    assertCommonHeaders(request);
                    assertPOSTHeaders(request);
                    return response.sendString(
                            request.receive().asString(StandardCharsets.UTF_8)
                                    .doOnNext(requestBody::set)
                                    .doOnComplete(completionSignal::countDown)
                    );
                })
                )
                .bindNow();

        ApiClient uut = createClient();

        final var props = new ProjectData.Properties(List.of("tag2", "tag4"), "my swid tag id", "my group", "my description", "parent-uuid", null, null, null);

        assertThatCode(() -> uut.updateProjectProperties("uuid-3", props)).doesNotThrowAnyException();
        completionSignal.await(5, TimeUnit.SECONDS);
        assertThat(completionSignal.getCount()).isZero();
        final JSONObject updatedProject = JSONObject.fromObject(requestBody.get());
        final Project project = ProjectParser.parse(updatedProject);
        assertThat(project.getTags()).containsExactlyInAnyOrderElementsOf(props.tags());
        assertThat(project.getSwidTagId()).isEqualTo(props.swidTagId());
        assertThat(project.getGroup()).isEqualTo(props.group());
        assertThat(project.getDescription()).isEqualTo(props.description());
        assertThat(project.getParent()).hasFieldOrPropertyWithValue("uuid", props.parentId());
        assertThat(updatedProject.has("parentUuid")).isFalse();

        assertThatCode(() -> createClient().updateProjectProperties("uuid-unknown", props))
                .hasMessage(Messages.ApiClient_Error_ProjectUpdate("uuid-unknown", HttpResponseStatus.NOT_FOUND.code(), HttpResponseStatus.NOT_FOUND.reasonPhrase()))
                .hasNoCause();
        verify(logger).log("");
    }

    @Test
    void updateProjectPropertiesTestWithStatus304(JenkinsRule r) throws InterruptedException {
        final CountDownLatch completionSignal = new CountDownLatch(1);
        server = HttpServer.create()
                .host("localhost")
                .port(0)
                .route(routes -> routes
                .route(request -> request.uri().equals(ApiClient.PROJECT_URL + "/uuid-3") && request.method().equals(HttpMethod.PATCH), (request, response) -> {
                    completionSignal.countDown();
                    return response.status(HttpResponseStatus.NOT_MODIFIED).send();
                })
                )
                .bindNow();

        ApiClient uut = createClient();
        final var props = new ProjectData.Properties(null, "swid", null, null, null, null, null, null);

        assertThatCode(() -> uut.updateProjectProperties("uuid-3", props)).doesNotThrowAnyException();
        completionSignal.await(5, TimeUnit.SECONDS);
        assertThat(completionSignal.getCount()).isZero();
    }

    @Test
    void updateProjectPropertiesTestWithErrorsOnUpdate() throws IOException {
        final var httpClient = mock(OkHttpClient.class);
        final var call = mock(okhttp3.Call.class);
        final var uut = createClient(httpClient);
        final var props = new ProjectData.Properties(null, "swid", null, null, null, null, null, null);
        when(httpClient.newCall(any(okhttp3.Request.class))).thenReturn(call);
        doThrow(new ConnectException("oops"))
                .when(call).execute();

        assertThatCode(() -> uut.updateProjectProperties("foo", props))
                .hasMessage(Messages.ApiClient_Error_Connection("", ""))
                .hasCauseInstanceOf(ConnectException.class);
        verify(httpClient, times(2)).newCall(any(okhttp3.Request.class));
    }

    @Test
    void updateProjectPropertiesTestWithEmptyProperties() throws IOException {
        final var httpClient = mock(OkHttpClient.class);
        final var uut = createClient(httpClient);

        uut.updateProjectProperties("foo", new ProjectData.Properties(null, null, null, null, null, null, null, null));

        verify(httpClient, never()).newCall(any(okhttp3.Request.class));
    }

    @Test
    void getTeamPermissionsTest(JenkinsRule r) throws ApiClientException {
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
    void getTeamPermissionsTestWithErrors() throws IOException {
        final var httpClient = mock(OkHttpClient.class);
        final var call = mock(okhttp3.Call.class);
        final var uut = createClient(httpClient);
        when(httpClient.newCall(any(okhttp3.Request.class))).thenReturn(call);
        doThrow(new ConnectException("oops"))
                .when(call).execute();

        assertThatCode(uut::getTeamPermissions)
                .hasMessage(Messages.ApiClient_Error_Connection("", ""))
                .hasCauseInstanceOf(ConnectException.class);
        verify(httpClient, times(2)).newCall(any(okhttp3.Request.class));
    }

    @Test
    void testGetVersion(JenkinsRule r) throws ApiClientException {
        server = HttpServer.create()
                .host("localhost")
                .port(0)
                .route(routes -> routes.get(ApiClient.VERSION_URL, (request, response) -> {
            return response.sendString(Mono.just("{\"version\":\"1.2.4\"}"));
        }))
                .bindNow();

        ApiClient uut = createClient();

        assertThat(uut.getVersion()).isEqualTo("1.2.4");

        server.disposeNow();
        server = HttpServer.create().host("localhost").port(0).route(routes -> routes.get(ApiClient.VERSION_URL, (request, response) -> response.status(HttpResponseStatus.BAD_REQUEST).send())).bindNow();
        assertThatCode(() -> createClient().getVersion())
                .hasMessage(Messages.ApiClient_Error_Connection(HttpResponseStatus.BAD_REQUEST.code(), HttpResponseStatus.BAD_REQUEST.reasonPhrase()))
                .hasNoCause();
        verify(logger).log("");
    }

    @Test
    void testGetVersionWithErrors() throws IOException {
        final var httpClient = mock(OkHttpClient.class);
        final var call = mock(okhttp3.Call.class);
        final var uut = createClient(httpClient);
        when(httpClient.newCall(any(okhttp3.Request.class))).thenReturn(call);
        doThrow(new ConnectException("oops"))
                .when(call).execute();

        assertThatCode(uut::getVersion)
                .hasMessage(Messages.ApiClient_Error_Connection("", ""))
                .hasCauseInstanceOf(ConnectException.class);
        verify(httpClient, times(2)).newCall(any(okhttp3.Request.class));
    }

    @Test
    void testWithContextPath(JenkinsRule r) {
        server = HttpServer.create()
                .host("localhost")
                .port(0)
                .route(routes -> routes.get("/ctx" + ApiClient.PROJECT_URL, (request, response) -> response.status(HttpResponseStatus.INTERNAL_SERVER_ERROR).sendString(Mono.just("something went wrong"))))
                .bindNow();

        ApiClient uut = new ApiClient(String.format("http://%s:%d/ctx", server.host(), server.port()), API_KEY, logger, new OkHttpClient());

        assertThatCode(uut::testConnection).isInstanceOf(ApiClientException.class)
                .hasNoCause()
                // if context path would be ignores, the server would return 404 instead of 500
                .hasMessage(Messages.ApiClient_Error_Connection(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase()));
    }

}
