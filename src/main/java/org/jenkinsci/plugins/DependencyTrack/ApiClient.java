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

import hudson.FilePath;
import hudson.remoting.Base64;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

public class ApiClient {

    private static final String API_KEY_HEADER = "X-Api-Key";
    private static final String PROJECT_FINDINGS_URL = "/api/v1/finding/project";
    private static final String BOM_TOKEN_URL = "/api/v1/bom/token";
    private static final String BOM_UPLOAD_URL = "/api/v1/bom";
    private static final String SCAN_UPLOAD_URL = "/api/v1/scan";
    private static final String SEARCH_URL = "/api/v1/project";
	private static final Object EQUAL_SIGN = "=";

    private final String baseUrl;
    private final String apiKey;
    private final ConsoleLogger logger;

    public ApiClient(final String baseUrl, final String apiKey, final ConsoleLogger logger) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.logger = logger;
    }

    public String getFindings(String projectUuid) throws ApiClientException {
        try {
            final HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + PROJECT_FINDINGS_URL + "/" + projectUuid)
                    .openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            conn.setRequestProperty(API_KEY_HEADER, apiKey);
            conn.connect();
            // Checks the server response
            if (conn.getResponseCode() == 200) {
                return getResponseBody(conn.getInputStream());
            } else {
                throw new ApiClientException("An error occurred while retrieving findings - HTTP response code: " + conn.getResponseCode() + " " + conn.getResponseMessage());
            }
        } catch (IOException e) {
            throw new ApiClientException("An error occurred while retrieving findings", e);
        }
    }
    
	public String getFindings(String projectName, String version) throws ApiClientException {
		String projectUuid = getProjectUUID(projectName, version);
		return getFindings(projectUuid);
	}

	private String getProjectUUID(String projectName, String version) throws ApiClientException {
		try {
			Map<String, Object> params = new HashMap<>(1);
			params.put("searchText", projectName);
			final HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + SEARCH_URL + "?" + getQuery(params)).openConnection();
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestMethod("GET");
			conn.setRequestProperty(API_KEY_HEADER, apiKey);			
			conn.connect();
			if (conn.getResponseCode() == 200) {
				JsonReader reader = Json.createReader(new StringReader(getResponseBody(conn.getInputStream())));
				JsonArray projectResponse = reader.readArray();
				reader.close();
				for (JsonValue jsonValue : projectResponse) {
					JsonReader tempReader = Json.createReader(new StringReader(jsonValue.toString()));
					JsonObject object = tempReader.readObject();
					tempReader.close();
					if(object.getString("version").equalsIgnoreCase(version)) {
						return object.getString("uuid");
					}
				}
            } else {
                throw new ApiClientException("An error occurred while retrieving findings - HTTP response code: " + conn.getResponseCode() + " " + conn.getResponseMessage());
            }
		} catch (IOException ex) {
			throw new ApiClientException("An error occurred while retrieving findings", ex);
		}
		return null;
	}
	
	private String getQuery(Map<String, Object> params) throws ApiClientException {
		StringBuilder builder = new StringBuilder();
		try {			
			for (Entry<String, Object> entry : params.entrySet()) {
				Object value = entry.getValue();
				if ( null == value ) 
					continue;
				builder.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
				builder.append(EQUAL_SIGN);
				builder.append(URLEncoder.encode(value.toString(), "UTF-8"));
			}
		} catch (UnsupportedEncodingException ex) {
			throw new ApiClientException("An UnsupportedEncodingException occurred while retrieving findings", ex);
		}
		return builder.toString();
	}

	public UploadResult upload(String projectId, String projectName, String projectVersion, FilePath artifact,
                          boolean isScanResult, boolean autoCreateProject) throws IOException {
        final String encodedScan;
        try {
            encodedScan = Base64.encode(artifact.readToString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException | InterruptedException e) {
            logger.log(Messages.Builder_Error_Processing() + ": " + e.getMessage());
            return new UploadResult(false);
        }
        String uploadUrl = isScanResult ? SCAN_UPLOAD_URL : BOM_UPLOAD_URL;
        String jsonAttribute = isScanResult ? "scan" : "bom";
        // Creates the JSON payload that will be sent to Dependency-Track
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        if (projectId != null) {
            jsonObjectBuilder.add("project", projectId);
        } else {
            jsonObjectBuilder.add("projectName", projectName);
            jsonObjectBuilder.add("projectVersion", projectVersion);
            jsonObjectBuilder.add("autoCreate", autoCreateProject);
        }
        jsonObjectBuilder.add(jsonAttribute, encodedScan);
        JsonObject jsonObject = jsonObjectBuilder.build();
        byte[] payloadBytes = jsonObject.toString().getBytes(StandardCharsets.UTF_8);
        // Creates the request and connects
        final HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + uploadUrl).openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Content-Length", Integer.toString(payloadBytes.length));
        conn.setRequestProperty(API_KEY_HEADER, apiKey);
        conn.connect();
        // Sends the payload bytes
        try (OutputStream os = new BufferedOutputStream(conn.getOutputStream())) {
            os.write(payloadBytes);
            os.flush();
        }
        // Checks the server response
        if (conn.getResponseCode() == 200) {
            if (projectId != null) {
                logger.log(Messages.Builder_Success() + " - " + projectId);
            } else {
                logger.log(Messages.Builder_Success());
            }
            final String responseBody = getResponseBody(conn.getInputStream());
            if (StringUtils.isNotBlank(responseBody)) {
                final JSONObject json = JSONObject.fromObject(responseBody);
                return new UploadResult(true, UUID.fromString(json.getString("token")));
            } else {
                return new UploadResult(true);
            }
        } else if (conn.getResponseCode() == 400) {
            logger.log(Messages.Builder_Payload_Invalid());
        } else if (conn.getResponseCode() == 401) {
            logger.log(Messages.Builder_Unauthorized());
        } else if (conn.getResponseCode() == 404) {
            logger.log(Messages.Builder_Project_NotFound());
        } else {
            logger.log(Messages.Builder_Error_Connect() + ": "
                    + conn.getResponseCode() + " " + conn.getResponseMessage());
        }
        return new UploadResult(false);
    }

    public boolean isTokenBeingProcessed(UUID uuid) throws ApiClientException {
        try {
            final HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + BOM_TOKEN_URL + "/" + uuid.toString())
                    .openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            conn.setRequestProperty(API_KEY_HEADER, apiKey);
            conn.connect();
            if (conn.getResponseCode() == 200) {
                try (InputStream in = new BufferedInputStream(conn.getInputStream())) {
                    final JsonReader jsonReader = Json.createReader(in);
                    final JsonObject jsonObject = jsonReader.readObject();
                    final JsonValue value = jsonObject.get("processing");
                    return value == JsonValue.TRUE;
                }
            } else {
                logger.log("An acceptable response was not returned: " + conn.getResponseCode());
                throw new ApiClientException("An acceptable response was not returned: " + conn.getResponseCode());
            }
        } catch (IOException e) {
            throw new ApiClientException("An error occurred while checking if a token is being processed", e);
        }
    }

    private String getResponseBody(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        String line;
        while((line = reader.readLine()) != null) {
            result.append(line);
        }
        return result.toString();
    }

    public static class UploadResult {
        private boolean success;
        private UUID token;

        UploadResult(boolean success) {
            this.success = success;
            this.token = null;
        }
        UploadResult(boolean success, UUID token) {
            this.success = success;
            this.token = token;
        }
        public boolean isSuccess() {
            return success;
        }
        public UUID getToken() {
            return token;
        }
    }
}
