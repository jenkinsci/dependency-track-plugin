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

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.DependencyTrack.model.Finding;
import org.jenkinsci.plugins.DependencyTrack.model.RiskGate;
import org.jenkinsci.plugins.DependencyTrack.model.SeverityDistribution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import javax.annotation.Nonnull;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

@SuppressWarnings("unused")
public class DependencyTrackPublisher extends ThresholdCapablePublisher implements SimpleBuildStep, Serializable {

    private static final long serialVersionUID = 480115440498217963L;

    private String projectId;
    private String projectName;
    private String projectVersion;
    private final String artifact;
    private final String artifactType;
    private final boolean isScanResult;
    private final boolean synchronous;

    // Fields in config.jelly must match the parameter names
    @DataBoundConstructor
    public DependencyTrackPublisher(final String artifact, final String artifactType, final boolean synchronous) {
        this.artifact = artifact;
        this.artifactType = artifactType;
        this.isScanResult = artifactType == null || !"bom".equals(artifactType);
        this.synchronous = synchronous;

        this.projectId = null;
        this.projectName = null;
        this.projectVersion = null;
    }

    /**
     * Sets the project ID to upload to. This is a per-build config item. This
     * method must match the value in <tt>config.jelly</tt>.
     **/
    @DataBoundSetter
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    /**
     * Sets the project name to upload to. This is a per-build config item.
     **/
    @DataBoundSetter
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     * Sets the project name to upload to. This is a per-build config item.
     **/
    @DataBoundSetter
    public void setProjectVersion(String projectVersion) {
        this.projectVersion = projectVersion;
    }

    /**
     * Retrieves the project ID to upload to. This is a per-build config item.
     * This method must match the value in <tt>config.jelly</tt>.
     */
    public String getProjectId() {
        return projectId;
    }

    /**
     * Retrieves the path and filename of the artifact. This is a per-build config item.
     * This method must match the value in <tt>config.jelly</tt>.
     */
    public String getArtifact() {
        return artifact;
    }

    /**
     * Retrieves the type of artifact (bom or scanResult). This is a per-build config item.
     * This method must match the value in <tt>config.jelly</tt>.
     */
    public String getArtifactType() {
        return artifactType;
    }


    /**
     * Retrieves the project name to upload to. This is a per-build config item.
     * This method must match the value in <tt>config.jelly</tt>.
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * Retrieves the project version to upload to. This is a per-build config item.
     * This method must match the value in <tt>config.jelly</tt>.
     */
    public String getProjectVersion() {
        return projectVersion;
    }

    /**
     * Retrieves is synchronous mode is enabled or not. This is a per-build config item.
     * This method must match the value in <tt>config.jelly</tt>.
     */
    public boolean isSynchronous() {
        return synchronous;
    }

    /**
     * This method is called whenever the build step is executed.
     *
     * @param build    A Run object
     * @param filePath A FilePath object
     * @param launcher A Launcher object
     * @param listener A BuildListener object
     */
    @Override
    public void perform(@Nonnull final Run<?, ?> build,
                        @Nonnull final FilePath filePath,
                        @Nonnull final Launcher launcher,
                        @Nonnull final TaskListener listener) throws InterruptedException, IOException {

        ConsoleLogger logger = new ConsoleLogger(listener);
        final ApiClient apiClient = new ApiClient(
                getDescriptor().getDependencyTrackUrl(), getDescriptor().getDependencyTrackApiKey(), logger);

        logger.log(Messages.Builder_Publishing() + " - " + getDescriptor().getDependencyTrackUrl());

        final String projectId = build.getEnvironment(listener).expand(this.projectId);
        final String artifact = build.getEnvironment(listener).expand(this.artifact);
        final boolean autoCreateProject = getDescriptor().isDependencyTrackAutoCreateProjects();

        if (StringUtils.isBlank(artifact)) {
            logger.log(Messages.Builder_Artifact_Unspecified());
            build.setResult(Result.FAILURE);
            return;
        }
        final FilePath artifactFilePath = new FilePath(filePath, artifact);
        if (projectId == null && (projectName == null || projectVersion == null)) {
            logger.log(Messages.Builder_Result_InvalidArguments());
            build.setResult(Result.FAILURE);
            return;
        }
        try {
            if (!filePath.exists()) {
                logger.log(Messages.Builder_Artifact_NonExist());
                build.setResult(Result.FAILURE);
                return;
            }
            final ApiClient.UploadResult uploadResult = apiClient.upload(projectId, projectName, projectVersion,
                    artifactFilePath, isScanResult, autoCreateProject);

            if (!uploadResult.isSuccess()) {
                build.setResult(Result.FAILURE); //todo: make configurable
                return;
            }

            if (synchronous && isScanResult) {
                logger.log(Messages.Builder_Artifact_NonBomSync());
                return;
            }

            if (uploadResult.getToken() != null && synchronous) {
                final long timeout = System.currentTimeMillis() + (60000L * getDescriptor().getDependencyTrackPollingTimeout());
                Thread.sleep(10000);
                logger.log(Messages.Builder_Polling());
                while (apiClient.isTokenBeingProcessed(uploadResult.getToken())) {
                    Thread.sleep(10000);
                    if (timeout < System.currentTimeMillis()) {
                        logger.log(Messages.Builder_Polling_Timeout_Exceeded());
                        return;
                    }
                    logger.log(Messages.Builder_Polling());
                }
                logger.log(Messages.Builder_Findings_Processing());
                String jsonResponseBody = null;
                if (null == this.projectId) {
                	jsonResponseBody = apiClient.getFindings(this.projectName, this.projectVersion);
                } else {
                	jsonResponseBody = apiClient.getFindings(this.projectId); 
                }
                final FindingParser parser = new FindingParser(build.getNumber(), jsonResponseBody).parse();
                final ArrayList<Finding> findings = parser.getFindings();
                final SeverityDistribution severityDistribution = parser.getSeverityDistribution();
                final ResultAction projectAction = new ResultAction(findings, severityDistribution);
                build.addAction(projectAction);

                // Get previous results and evaluate to thresholds
                final Run previousBuild = build.getPreviousBuild();
                final RiskGate riskGate = new RiskGate(getThresholds());
                if (previousBuild != null) {
                    final ResultAction previousResults = previousBuild.getAction(ResultAction.class);
                    if (previousResults != null) {
                        final Result result = riskGate.evaluate(
                                previousResults.getSeverityDistribution(),
                                previousResults.getFindings(),
                                severityDistribution,
                                findings);
                        evaluateRiskGates(build, logger, result);
                    } else { // Resolves https://issues.jenkins-ci.org/browse/JENKINS-58387
                        final Result result = riskGate.evaluate(severityDistribution, new ArrayList<>(), severityDistribution, findings);
                        evaluateRiskGates(build, logger, result);
                    }
                } else { // Resolves https://issues.jenkins-ci.org/browse/JENKINS-58387
                    final Result result = riskGate.evaluate(severityDistribution, new ArrayList<>(), severityDistribution, findings);
                    evaluateRiskGates(build, logger, result);
                }
            }
        } catch (ApiClientException e) {
            logger.log(e.getMessage());
            build.setResult(Result.FAILURE); //todo: make configurable
        }
    }

    private void evaluateRiskGates(final Run<?, ?> build, final ConsoleLogger logger, final Result result) {
        if (Result.SUCCESS != result) {
            logger.log(Messages.Builder_Threshold_Exceed());
            build.setResult(result); // only set the result if the evaluation fails the threshold
        }
    }

    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
        return new JobAction(project);
    }

    /**
     * A Descriptor Implementation.
     */
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * Descriptor for {@link DependencyTrackPublisher}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     * <p/>
     * <p/>
     * See <tt>src/main/resources/org/jenkinsci/plugins/DependencyCheck/DependencyTrackBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension @Symbol("dependencyTrackPublisher") // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> implements Serializable {

        private static final long serialVersionUID = -2018722914973282748L;

        /**
         * Specifies the base URL to Dependency-Track v3 or higher.
         */
        private String dependencyTrackUrl;

        /**
         * Specifies an API Key used for authentication (if authentication is required).
         */
        private String dependencyTrackApiKey;


        /**
         * Specifies whether the API key provided has the PROJECT_CREATION_UPLOAD permission.
         */
        private boolean dependencyTrackAutoCreateProjects;

        /**
         * Specifies the maximum number of minutes to wait for synchronous jobs to complete.
         */
        private int dependencyTrackPollingTimeout;

        /**
         * Default constructor. Obtains the Descriptor used in DependencyCheckBuilder as this contains
         * the global Dependency-Check Jenkins plugin configuration.
         */
        public DescriptorImpl() {
            super(DependencyTrackPublisher.class);
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        /**
         * Retrieve the projects to populate the dropdown.
         */
        public ListBoxModel doFillProjectIdItems() {
            final ListBoxModel projects = new ListBoxModel();
            try {
                // Creates the request and connects
                boolean isPaginated = true;
                int page = 1;
                while(isPaginated) {
                    final HttpURLConnection conn = (HttpURLConnection) new URL(getDependencyTrackUrl() + "/api/v1/project?limit=500&page="+page)
                            .openConnection();
                    conn.setDoOutput(true);
                    conn.setDoInput(true);
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("X-Api-Key", getDependencyTrackApiKey());
                    conn.connect();

                    // Checks the server response
                    if (conn.getResponseCode() == 200) {
                        try (InputStream in = new BufferedInputStream(conn.getInputStream())) {
                            JsonReader jsonReader = Json.createReader(in);
                            JsonArray array = jsonReader.readArray();
                            // Add an empty option at the beginning of the list
                            if (projects.size() == 0) {
                                projects.add("-- Select Project --", null);
                            }
                            if (!array.isEmpty()) {
                                for (int i = 0; i < array.size(); i++) {
                                    final JsonObject jsonObject = array.getJsonObject(i);
                                    final StringBuilder nameBuilder = new StringBuilder();
                                    final String name = jsonObject.getString("name");
                                    final String version = jsonObject.getString("version", "null");
                                    final String uuid = jsonObject.getString("uuid");
                                    nameBuilder.append(name);
                                    if (!"null".equals(version)) {
                                        nameBuilder.append(' ').append(version);
                                    }
                                    projects.add(nameBuilder.toString(), uuid);
                                }
                            } else {
                                isPaginated = false;
                            }
                        }
                    } else {
                        projects.add(Messages.Builder_Error_Projects() + ": " + conn.getResponseCode());
                        isPaginated = false;
                    }
                    page++;
                }
            } catch (IOException e) {
                projects.add(e.getMessage());
            }
            return projects;
        }

        /**
         * Performs input validation when submitting the global config
         * @param value The value of the URL as specified in the global config
         * @return a FormValidation object
         */
        public FormValidation doCheckDependencyTrackUrl(@QueryParameter String value) {
            return PluginUtil.doCheckUrl(value);
        }

        /**
         * Performs an on-the-fly check of the Dependency-Track URL and api key
         * parameters by making a simple call to the server and validating
         * the response code.
         * @param dependencyTrackUrl the base URL to Dependency-Track
         * @param dependencyTrackApiKey the API key to use for authentication
         * @return FormValidation
         */
        public FormValidation doTestConnection(@QueryParameter final String dependencyTrackUrl,
                                               @QueryParameter final String dependencyTrackApiKey) {
            try {
                final String baseUrl = PluginUtil.parseBaseUrl(dependencyTrackUrl);
                final HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + "/api/v1/project").openConnection();
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("X-Api-Key", dependencyTrackApiKey);
                conn.connect();
                if (conn.getResponseCode() == 200) {
                    final String serverId = conn.getHeaderField("X-Powered-By");
                    return FormValidation.ok("Connection successful - " + serverId);
                } else if (conn.getResponseCode() == 401) {
                    return FormValidation.warning("Authentication or authorization failure");
                }
            } catch (Exception e) {
                return FormValidation.error(e, "An error occurred connecting to Dependency-Track");
            }
            return FormValidation.error("An error occurred connecting to Dependency-Track");
        }

        /**
         * Takes the /apply/save step in the global config and saves the JSON data.
         * @param req the request
         * @param formData the form data
         * @return a boolean
         * @throws FormException an exception validating form input
         */
        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            dependencyTrackUrl = formData.getString("dependencyTrackUrl");
            dependencyTrackApiKey = formData.getString("dependencyTrackApiKey");
            dependencyTrackAutoCreateProjects = formData.getBoolean("dependencyTrackAutoCreateProjects");
            dependencyTrackPollingTimeout = formData.getInt("dependencyTrackPollingTimeout");
            save();
            return super.configure(req, formData);
        }

        /**
         * This name is used on the build configuration screen.
         */
        @Override
        public String getDisplayName() {
            return Messages.Publisher_DependencyTrack_Name();
        }

        /**
         * This method returns the global configuration for dependencyTrackUrl.
         */
        public String getDependencyTrackUrl() {
            return PluginUtil.parseBaseUrl(dependencyTrackUrl);
        }

        /**
         * This method returns the global configuration for dependencyTrackApiKey.
         */
        public String getDependencyTrackApiKey() {
            return dependencyTrackApiKey;
        }

        /**
         * This method returns the global configuration for
         * dependencyTrackAutoCreateProjects.
         */
        public boolean isDependencyTrackAutoCreateProjects() {
            return dependencyTrackAutoCreateProjects;
        }

        /**
         * This method returns the global configuration for dependencyTrackPollingTimeout.
         */
        public int getDependencyTrackPollingTimeout() {
            if (dependencyTrackPollingTimeout <= 0) {
                return 5;
            }
            return dependencyTrackPollingTimeout;
        }
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

}
