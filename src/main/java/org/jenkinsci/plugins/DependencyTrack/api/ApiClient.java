/*
 * This file is part of Dependency-Track Jenkins plugin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jenkinsci.plugins.DependencyTrack.api;

import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.DependencyTrack.model.Finding;
import org.jenkinsci.plugins.DependencyTrack.model.FindingParser;
import org.jenkinsci.plugins.DependencyTrack.model.Project;
import org.jenkinsci.plugins.DependencyTrack.model.ProjectParser;
import org.jenkinsci.plugins.DependencyTrack.model.Team;
import org.jenkinsci.plugins.DependencyTrack.model.TeamParser;
import org.jenkinsci.plugins.DependencyTrack.model.Violation;
import org.jenkinsci.plugins.DependencyTrack.model.ViolationParser;
import org.springframework.http.HttpStatus;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.UniformRandomBackOffPolicy;
import org.springframework.retry.policy.BinaryExceptionClassifierRetryPolicy;
import org.springframework.retry.policy.CompositeRetryPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RequiredArgsConstructor
public class ApiClient {

    private static final MediaType APPLICATION_JSON = MediaType.parse(APPLICATION_JSON_VALUE);
    private static final String API_URL = "/api/v1";
    static final String API_KEY_HEADER = "X-Api-Key";
    static final String PROJECT_FINDINGS_URL = API_URL + "/finding/project";
    static final String PROJECT_VIOLATIONS_URL = API_URL + "/violation/project";
    static final String BOM_URL = API_URL + "/bom";
    static final String BOM_TOKEN_URL = API_URL + "/event/token";
    static final String PROJECT_URL = API_URL + "/project";
    static final String PROJECT_LOOKUP_URL = PROJECT_URL + "/lookup";
    static final String PROJECT_LOOKUP_NAME_PARAM = "name";
    static final String PROJECT_LOOKUP_VERSION_PARAM = "version";
    static final String TEAM_SELF_URL = API_URL + "/team/self";
    static final String VERSION_URL = "/api/version";

    /**
     * the base url to DT instance without trailing slashes, e.g.
     * "http://host.tld:port"
     */
    @Nonnull
    private final String baseUrl;

    /**
     * the api key to authorize with against DT
     */
    @Nonnull
    private final String apiKey;

    @Nonnull
    private final Logger logger;

    @Nonnull
    private final OkHttpClient httpClient;

    @Nonnull
    public String getVersion() throws ApiClientException {
        final var request = createRequest(URI.create(VERSION_URL));
        return executeWithRetry(() -> {
            try (var response = httpClient.newCall(request).execute()) {
                final var body = response.body().string();
                if (!response.isSuccessful()) {
                    final int status = response.code();
                    logger.log(body);
                    throw new ApiClientException(Messages.ApiClient_Error_Connection(status, HttpStatus.valueOf(status).getReasonPhrase()));
                }
                final var jsonObject = JSONObject.fromObject(body);
                return jsonObject.getString("version");
            } catch (ApiClientException e) {
                throw e;
            } catch (IOException e) {
                throw new ApiClientException(Messages.ApiClient_Error_Connection(StringUtils.EMPTY, StringUtils.EMPTY), e);
            }
        });
    }

    @Nonnull
    public String testConnection() throws ApiClientException {
        final var request = createRequest(URI.create(PROJECT_URL));
        return executeWithRetry(() -> {
            try (var response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    return StringUtils.trim(response.header("X-Powered-By", StringUtils.EMPTY));
                } else {
                    final int status = response.code();
                    logger.log(response.body().string());
                    throw new ApiClientException(Messages.ApiClient_Error_Connection(status, HttpStatus.valueOf(status).getReasonPhrase()));
                }
            } catch (ApiClientException e) {
                throw e;
            } catch (IOException e) {
                throw new ApiClientException(Messages.ApiClient_Error_Connection(StringUtils.EMPTY, StringUtils.EMPTY), e);
            }
        });
    }

    @Nonnull
    public Team getTeamPermissions() throws ApiClientException {
        final var request = createRequest(URI.create(TEAM_SELF_URL));
        return executeWithRetry(() -> {
            try (var response = httpClient.newCall(request).execute()) {
                final var body = response.body().string();
                if (!response.isSuccessful()) {
                    final int status = response.code();
                    logger.log(body);
                    throw new ApiClientException(Messages.ApiClient_Error_Connection(status, HttpStatus.valueOf(status).getReasonPhrase()));
                }
                final var jsonObject = JSONObject.fromObject(body);
                return TeamParser.parse(jsonObject);
            } catch (ApiClientException e) {
                throw e;
            } catch (IOException e) {
                throw new ApiClientException(Messages.ApiClient_Error_Connection(StringUtils.EMPTY, StringUtils.EMPTY), e);
            }
        });
    }

    @Nonnull
    public List<Project> getProjects() throws ApiClientException {
        List<Project> projects = new ArrayList<>();
        int page = 1;
        boolean fetchMore = true;
        while (fetchMore) {
            List<Project> fetchedProjects = getProjectsPaged(page++);
            fetchMore = !fetchedProjects.isEmpty();
            projects.addAll(fetchedProjects);
        }
        return projects;
    }

    @Nonnull
    private List<Project> getProjectsPaged(final int page) throws ApiClientException {
        final var uri = UriComponentsBuilder.fromUriString(PROJECT_URL)
                .queryParam("limit", "{limit}")
                .queryParam("excludeInactive", "{excludeInactive}")
                .queryParam("page", "{page}")
                .build(500, true, page);
        final var request = createRequest(uri);
        return executeWithRetry(() -> {
            try (var response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    return JSONArray.fromObject(response.body().string()).stream()
                            .map(JSONObject.class::cast)
                            .map(ProjectParser::parse)
                            .toList();
                }
                return List.of();
            } catch (IOException e) {
                throw new ApiClientException(Messages.ApiClient_Error_Connection(StringUtils.EMPTY, StringUtils.EMPTY), e);
            }
        });
    }

    @Nonnull
    public Project lookupProject(@Nonnull final String projectName, @Nonnull final String projectVersion) throws ApiClientException {
        final var uri = UriComponentsBuilder.fromUriString(PROJECT_LOOKUP_URL)
                .queryParam(PROJECT_LOOKUP_NAME_PARAM, "{name}")
                .queryParam(PROJECT_LOOKUP_VERSION_PARAM, "{version}")
                .build(projectName, projectVersion);
        final var request = createRequest(uri);
        return executeWithRetry(() -> {
            try (var response = httpClient.newCall(request).execute()) {
                final var body = response.body().string();
                if (!response.isSuccessful()) {
                    final int status = response.code();
                    logger.log(body);
                    throw new ApiClientException(Messages.ApiClient_Error_ProjectLookup(projectName, projectVersion, status, HttpStatus.valueOf(status).getReasonPhrase()));
                }
                final var jsonObject = JSONObject.fromObject(body);
                final var version = jsonObject.getString("version");
                final var builder = Project.builder()
                        .name(jsonObject.getString("name"))
                        .uuid(jsonObject.getString("uuid"));
                if (StringUtils.isNotBlank(version) && !"null".equalsIgnoreCase(version)) {
                    builder.version(version);
                }
                return builder.build();
            } catch (ApiClientException e) {
                throw e;
            } catch (IOException e) {
                throw new ApiClientException(Messages.ApiClient_Error_Connection(StringUtils.EMPTY, StringUtils.EMPTY), e);
            }
        });
    }

    @Nonnull
    public List<Finding> getFindings(@Nonnull final String projectUuid) throws ApiClientException {
        final var uri = UriComponentsBuilder.fromUriString(PROJECT_FINDINGS_URL).pathSegment("{uuid}").build(projectUuid);
        final var request = createRequest(uri);
        return executeWithRetry(() -> {
            try (var response = httpClient.newCall(request).execute()) {
                final var body = response.body().string();
                if (!response.isSuccessful()) {
                    final int status = response.code();
                    logger.log(body);
                    throw new ApiClientException(Messages.ApiClient_Error_RetrieveFindings(status, HttpStatus.valueOf(status).getReasonPhrase()));
                }
                return FindingParser.parse(body);
            } catch (ApiClientException e) {
                throw e;
            } catch (IOException e) {
                throw new ApiClientException(Messages.ApiClient_Error_Connection(StringUtils.EMPTY, StringUtils.EMPTY), e);
            }
        });
    }

    @Nonnull
    public List<Violation> getViolations(@Nonnull final String projectUuid) throws ApiClientException {
        final var uri = UriComponentsBuilder.fromUriString(PROJECT_VIOLATIONS_URL).pathSegment("{uuid}").build(projectUuid);
        final var request = createRequest(uri);
        return executeWithRetry(() -> {
            try (var response = httpClient.newCall(request).execute()) {
                final var body = response.body().string();
                if (!response.isSuccessful()) {
                    final int status = response.code();
                    logger.log(body);
                    throw new ApiClientException(Messages.ApiClient_Error_RetrieveViolations(status, HttpStatus.valueOf(status).getReasonPhrase()));
                }
                return ViolationParser.parse(body);
            } catch (ApiClientException e) {
                throw e;
            } catch (IOException e) {
                throw new ApiClientException(Messages.ApiClient_Error_Connection(StringUtils.EMPTY, StringUtils.EMPTY), e);
            }
        });
    }

    @Nonnull
    public UploadResult upload(@Nonnull final ProjectData project, @Nonnull final String bom) throws ApiClientException {
        final var formBodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        formBodyBuilder.addFormDataPart("bom", bom);
        // Creates the JSON payload that will be sent to Dependency-Track
        if (StringUtils.isNotBlank(project.id())) {
            formBodyBuilder.addFormDataPart("project", project.id());
        } else {
            formBodyBuilder.addFormDataPart("projectName", project.name())
                    .addFormDataPart("projectVersion", project.version())
                    .addFormDataPart("autoCreate", String.valueOf(project.autoCreate()));
        }
        final var properties = project.properties();
        if (properties != null) {
            Optional.ofNullable(properties.parentId()).ifPresent(value -> formBodyBuilder.addFormDataPart("parentUUID", value));
            Optional.ofNullable(properties.parentName()).ifPresent(value -> formBodyBuilder.addFormDataPart("parentName", value));
            Optional.ofNullable(properties.parentVersion()).ifPresent(value -> formBodyBuilder.addFormDataPart("parentVersion", value));
            Optional.ofNullable(properties.isLatest()).map(String::valueOf).ifPresent(value -> formBodyBuilder.addFormDataPart("isLatest", value));
        }
        final var request = createRequest(URI.create(BOM_URL), "POST", formBodyBuilder.build());
        return executeWithRetry(() -> {
            try (var response = httpClient.newCall(request).execute()) {
                final var body = response.body().string();
                final int status = response.code();
                // Checks the server response
                switch (status) {
                    case HTTP_OK -> {
                        if (StringUtils.isNotBlank(body)) {
                            final var json = JSONObject.fromObject(body);
                            return new UploadResult(true, StringUtils.trimToNull(json.getString("token")));
                        } else {
                            return new UploadResult(true);
                        }
                    }
                    case HTTP_BAD_REQUEST ->
                        logger.log(Messages.ApiClient_Payload_Invalid());
                    case HTTP_UNAUTHORIZED ->
                        logger.log(Messages.ApiClient_Unauthorized());
                    case HTTP_NOT_FOUND ->
                        logger.log(Messages.ApiClient_Project_NotFound());
                    default ->
                        logger.log(Messages.ApiClient_Error_Connection(status, HttpStatus.valueOf(status).getReasonPhrase()));
                }
                logger.log(body);
                return new UploadResult(false);
            } catch (IOException e) {
                throw new ApiClientException(Messages.ApiClient_Error_Connection(StringUtils.EMPTY, StringUtils.EMPTY), e);
            }
        });
    }

    public void updateProjectProperties(@Nonnull final String projectUuid, @Nonnull final ProjectData.Properties properties) throws ApiClientException {
        final var updates = new JSONObject();
        final var tags = (properties.tags() != null ? properties.tags().stream() : Stream.empty())
                .map(tag -> Map.of("name", tag))
                .toList();
        // overwrite tags if needed
        if (!tags.isEmpty()) {
            updates.element("tags", tags);
        }
        // overwrite swidTagId only if it is set (means not null)
        updates.elementOpt("swidTagId", properties.swidTagId());
        // overwrite group only if it is set (means not null)
        updates.elementOpt("group", properties.group());
        // overwrite description only if it is set (means not null)
        updates.elementOpt("description", properties.description());
        // overwrite isLatest only if it is set (means not null)
        updates.elementOpt("isLatest", properties.isLatest());
        // set new parent project if it is set (means not null)
        if (properties.parentId() != null) {
            JSONObject newParent = new JSONObject().elementOpt("uuid", properties.parentId());
            updates.element("parent", newParent);
        }

        // update project if necessary
        if (!updates.isEmpty()) {
            updateProject(projectUuid, updates);
        }
    }

    private void updateProject(@Nonnull final String projectUuid, @Nonnull final JSONObject project) throws ApiClientException {
        final var uri = UriComponentsBuilder.fromUriString(PROJECT_URL).pathSegment("{uuid}").build(projectUuid);
        final var request = createRequest(uri, "PATCH", RequestBody.create(project.toString(), APPLICATION_JSON));
        executeWithRetry(() -> {
            try (var response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() && response.code() != HttpStatus.NOT_MODIFIED.value()) {
                    final var body = response.body().string();
                    final int status = response.code();
                    logger.log(body);
                    throw new ApiClientException(Messages.ApiClient_Error_ProjectUpdate(projectUuid, status, HttpStatus.valueOf(status).getReasonPhrase()));
                }
                return null;
            } catch (ApiClientException e) {
                throw e;
            } catch (IOException e) {
                throw new ApiClientException(Messages.ApiClient_Error_Connection(StringUtils.EMPTY, StringUtils.EMPTY), e);
            }
        });
    }

    public boolean isTokenBeingProcessed(@Nonnull final String token) throws ApiClientException {
        final var uri = UriComponentsBuilder.fromUriString(BOM_TOKEN_URL).pathSegment("{token}").build(token);
        final var request = createRequest(uri);
        return executeWithRetry(() -> {
            try (var response = httpClient.newCall(request).execute()) {
                final var body = response.body().string();
                if (!response.isSuccessful()) {
                    final int status = response.code();
                    logger.log(body);
                    throw new ApiClientException(Messages.ApiClient_Error_TokenProcessing(status, HttpStatus.valueOf(status).getReasonPhrase()));
                }
                final var jsonObject = JSONObject.fromObject(body);
                return jsonObject.getBoolean("processing");
            } catch (ApiClientException e) {
                throw e;
            } catch (IOException e) {
                throw new ApiClientException(Messages.ApiClient_Error_Connection(StringUtils.EMPTY, StringUtils.EMPTY), e);
            }
        });
    }

    private Request createRequest(final URI uri) {
        return createRequest(uri, "GET", null);
    }

    private Request createRequest(final URI uri, final String method, final RequestBody bodyPublisher) {
        return new Request.Builder()
                .url(baseUrl + uri)
                .addHeader(API_KEY_HEADER, apiKey)
                .addHeader(ACCEPT, APPLICATION_JSON_VALUE)
                .method(method, bodyPublisher)
                .build();
    }

    private <T, E extends IOException> T executeWithRetry(RetryAction<T, E> action) throws E {
        final var exceptionClassifier = new ApiClientExceptionClassifier();
        final var retryPolicy = new CompositeRetryPolicy();
        final var backOffPolicy = new UniformRandomBackOffPolicy();
        final var template = new RetryTemplate();

        backOffPolicy.setMinBackOffPeriod(50);
        backOffPolicy.setMaxBackOffPeriod(500);
        retryPolicy.setPolicies(new RetryPolicy[]{new MaxAttemptsRetryPolicy(2), new BinaryExceptionClassifierRetryPolicy(exceptionClassifier)});
        template.setBackOffPolicy(backOffPolicy);
        template.setRetryPolicy(retryPolicy);

        return template.execute(ctx -> action.doWithRetry());
    }

    private interface RetryAction<T, E extends IOException> {

        T doWithRetry() throws E;
    }
}
