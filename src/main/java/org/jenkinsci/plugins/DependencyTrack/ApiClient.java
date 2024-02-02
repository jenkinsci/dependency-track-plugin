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
package org.jenkinsci.plugins.DependencyTrack;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.FilePath;
import hudson.util.VersionNumber;
import io.jenkins.plugins.okhttp.api.JenkinsOkHttpClient;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.DependencyTrack.model.Finding;
import org.jenkinsci.plugins.DependencyTrack.model.Project;
import org.jenkinsci.plugins.DependencyTrack.model.Team;
import org.jenkinsci.plugins.DependencyTrack.model.UploadResult;
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

public class ApiClient {

    private static final String API_URL = "/api/v1";
    static final String API_KEY_HEADER = "X-Api-Key";
    static final String PROJECT_FINDINGS_URL = API_URL + "/finding/project";
    static final String PROJECT_POLICY_VIOLATION_URL = API_URL + "/violation/project";
    static final String BOM_URL = API_URL + "/bom";
    static final String BOM_TOKEN_URL = BOM_URL + "/token";
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
    private final String baseUrl;

    /**
     * the api key to authorize with against DT
     */
    private final String apiKey;

    private final ConsoleLogger logger;
    private final OkHttpClient httpClient;

    /**
     *
     * @param baseUrl the base url to DT instance without trailing slashes, e.g.
     * "http://host.tld:port"
     * @param apiKey the api key to authorize with against DT
     * @param logger
     * @param connectionTimeout the connection-timeout in seconds for every call
     * to DT
     * @param readTimeout the read-timeout in seconds for every call to DT
     */
    public ApiClient(@NonNull final String baseUrl, @NonNull final String apiKey, @NonNull final ConsoleLogger logger, final int connectionTimeout, final int readTimeout) {
        this(baseUrl, apiKey, logger, () -> JenkinsOkHttpClient.newClientBuilder(new OkHttpClient())
                .connectTimeout(Duration.ofSeconds(connectionTimeout))
                .readTimeout(Duration.ofSeconds(readTimeout))
                .build());
    }

    ApiClient(@NonNull final String baseUrl, @NonNull final String apiKey, @NonNull final ConsoleLogger logger, @NonNull final HttpClientFactory factory) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.logger = logger;
        httpClient = factory.create();
    }

    @NonNull
    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Returns a non-null value if this response was passed to Callback.onResponse or returned from Call.execute")
    public VersionNumber getVersion() throws ApiClientException {
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
                return new VersionNumber(jsonObject.getString("version"));
            } catch (ApiClientException e) {
                throw e;
            } catch (IOException e) {
                throw new ApiClientException(Messages.ApiClient_Error_Connection(StringUtils.EMPTY, StringUtils.EMPTY), e);
            }
        });
    }

    @NonNull
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
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

    @NonNull
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
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

    @NonNull
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

    @NonNull
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
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
                            .collect(Collectors.toList());
                }
                return List.of();
            } catch (IOException e) {
                throw new ApiClientException(Messages.ApiClient_Error_Connection(StringUtils.EMPTY, StringUtils.EMPTY), e);
            }
        });
    }

    @NonNull
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    public Project lookupProject(@NonNull final String projectName, @NonNull final String projectVersion) throws ApiClientException {
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

    @NonNull
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    public List<Finding> getFindings(@NonNull final String projectUuid) throws ApiClientException {
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

    @NonNull
    public List<PolicyViolation> getPolicyViolation(@NonNull final String projectUuid) throws ApiClientException {
        try {
            final var uri = UriComponentsBuilder.fromUriString(PROJECT_POLICY_VIOLATION_URL).pathSegment("{uuid}").build(projectUuid);
            final var request = createRequest(uri);
            final var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            final var body = response.body();
            final int status = response.statusCode();
            if (status != HTTP_OK) {
                logger.log(body);
                throw new ApiClientException(Messages.ApiClient_Error_RetrievePolicyViolations(status, HttpStatus.valueOf(status).getReasonPhrase()));
            }
            return PolicyViolationsParser.parse(body);
        } catch (ApiClientException e) {
            throw e;
        } catch (IOException e) {
            throw new ApiClientException(Messages.ApiClient_Error_RetrievePolicyViolations(StringUtils.EMPTY, StringUtils.EMPTY), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiClientException(Messages.ApiClient_Error_Canceled(), e);
        }
    }

    @NonNull
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    public UploadResult upload(@Nullable final String projectId, @Nullable final String projectName, @Nullable final String projectVersion, @NonNull final FilePath artifact,
            boolean autoCreateProject) throws IOException {
        final String encodedScan;
        try (var in = artifact.read()) {
            encodedScan = Base64.getEncoder().encodeToString(in.readAllBytes());
        } catch (IOException | InterruptedException e) {
            logger.log(Messages.Builder_Error_Processing(artifact.getRemote(), e.getLocalizedMessage()));
            return new UploadResult(false);
        }
        // Creates the JSON payload that will be sent to Dependency-Track
        JSONObject jsonObject = new JSONObject();
        jsonObject.element("bom", encodedScan);
        if (StringUtils.isNotBlank(projectId)) {
            jsonObject.element("project", projectId);
        } else {
            jsonObject.element("projectName", projectName)
                    .element("projectVersion", projectVersion)
                    .element("autoCreate", autoCreateProject);
        }
        final var request = createRequest(URI.create(BOM_URL), "PUT", RequestBody.create(jsonObject.toString(), okhttp3.MediaType.parse(APPLICATION_JSON_VALUE)));
        return executeWithRetry(() -> {
            try (var response = httpClient.newCall(request).execute()) {
                final var body = response.body().string();
                final int status = response.code();
                // Checks the server response
                switch (status) {
                    case HTTP_OK:
                        if (StringUtils.isNotBlank(body)) {
                            final var json = JSONObject.fromObject(body);
                            return new UploadResult(true, StringUtils.trimToNull(json.getString("token")));
                        } else {
                            return new UploadResult(true);
                        }
                    case HTTP_BAD_REQUEST:
                        logger.log(Messages.Builder_Payload_Invalid());
                        break;
                    case HTTP_UNAUTHORIZED:
                        logger.log(Messages.Builder_Unauthorized());
                        break;
                    case HTTP_NOT_FOUND:
                        logger.log(Messages.Builder_Project_NotFound());
                        break;
                    default:
                        logger.log(Messages.ApiClient_Error_Connection(status, HttpStatus.valueOf(status).getReasonPhrase()));
                        break;
                }
                logger.log(body);
                return new UploadResult(false);
            }
        });

    }

    public void updateProjectProperties(@NonNull final String projectUuid, @NonNull final ProjectProperties properties) throws ApiClientException {
        final var updates = new JSONObject();
        final var tags = properties.getTags().stream()
                .map(tag -> Map.of("name", tag))
                .collect(Collectors.toList());
        // overwrite tags if needed
        if (!tags.isEmpty()) {
            updates.element("tags", tags);
        }
        // overwrite swidTagId only if it is set (means not null)
        updates.elementOpt("swidTagId", properties.getSwidTagId());
        // overwrite group only if it is set (means not null)
        updates.elementOpt("group", properties.getGroup());
        // overwrite description only if it is set (means not null)
        updates.elementOpt("description", properties.getDescription());
        // set new parent project if it is set (means not null)
        if (properties.getParentId() != null) {
            JSONObject newParent = new JSONObject().elementOpt("uuid", properties.getParentId());
            updates.element("parent", newParent);
        }

        // update project
        updateProject(projectUuid, updates);
    }

    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    private void updateProject(@NonNull final String projectUuid, @NonNull final JSONObject project) throws ApiClientException {
        final var uri = UriComponentsBuilder.fromUriString(PROJECT_URL).pathSegment("{uuid}").build(projectUuid);
        final var request = createRequest(uri, "PATCH", RequestBody.create(project.toString(), okhttp3.MediaType.parse(APPLICATION_JSON_VALUE)));
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

    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    public boolean isTokenBeingProcessed(@NonNull final String token) throws ApiClientException {
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
