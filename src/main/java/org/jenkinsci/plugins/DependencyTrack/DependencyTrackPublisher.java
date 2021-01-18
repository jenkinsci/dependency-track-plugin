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

import com.cloudbees.plugins.credentials.CredentialsProvider;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepMonitor;
import hudson.util.Secret;
import jenkins.tasks.SimpleBuildStep;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.DependencyTrack.model.*;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;

@Getter
@Setter(onMethod_ = {@DataBoundSetter})
@EqualsAndHashCode(callSuper = true)
public final class DependencyTrackPublisher extends ThresholdCapablePublisher implements SimpleBuildStep, Serializable {

    private static final long serialVersionUID = 480115440498217963L;

    /**
     * the project ID to upload to. This is a per-build config item.
     */
    private String projectId;

    /**
     * the project name to upload to. This is a per-build config item.
     */
    private String projectName;

    /**
     * the project version to upload to. This is a per-build config item.
     */
    private String projectVersion;

    /**
     * Retrieves the path and filename of the artifact. This is a per-build
     * config item.
     */
    private final String artifact;

    /**
     * Retrieves whether synchronous mode is enabled or not. This is a per-build
     * config item.
     */
    private final boolean synchronous;

    /**
     * Specifies the base URL to Dependency-Track v3 or higher.
     */
    private String dependencyTrackUrl;

    /**
     * Specifies the base URL to Dependency-Track API-Server v4 or higher.
     */
    private String dependencyTrackApiUrl;

    /**
     * Specifies the credential-id for an API Key used for authentication.
     */
    private String dependencyTrackApiKey;

    /**
     * Specifies whether the API key provided has the PROJECT_CREATION_UPLOAD
     * permission.
     */
    private Boolean autoCreateProjects;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private transient ApiClientFactory clientFactory;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private transient DescriptorImpl descriptor;

    private transient boolean overrideGlobals;

    // Fields in config.jelly must match the parameter names
    @DataBoundConstructor
    public DependencyTrackPublisher(final String artifact, final boolean synchronous) {
        this(artifact, synchronous, ApiClient::new);
    }

    DependencyTrackPublisher(String artifact, boolean synchronous, @lombok.NonNull ApiClientFactory clientFactory) {
        this.artifact = artifact;
        this.synchronous = synchronous;
        this.clientFactory = clientFactory;
        descriptor = getDescriptor();
    }

    /**
     * This method is called whenever the build step is executed.
     *
     * @param run       a build this is running as a part of
     * @param workspace a workspace to use for any file operations
     * @param env       environment variables applicable to this step
     * @param launcher  a way to start processes
     * @param listener  a place to send output
     * @throws InterruptedException if the step is interrupted
     * @throws IOException          if something goes wrong
     */
    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull EnvVars env, @NonNull Launcher launcher, @NonNull TaskListener listener) throws InterruptedException, IOException {
        final ConsoleLogger logger = new ConsoleLogger(listener.getLogger());
        final String effectiveProjectName = env.expand(projectName);
        final String effectiveProjectVersion = env.expand(projectVersion);
        final String effectiveArtifact = env.expand(artifact);

        if (StringUtils.isBlank(effectiveArtifact)) {
            logger.log(Messages.Builder_Artifact_Unspecified());
            throw new AbortException(Messages.Builder_Artifact_Unspecified());
        }
        if (StringUtils.isBlank(projectId) && (StringUtils.isBlank(effectiveProjectName) || StringUtils.isBlank(effectiveProjectVersion))) {
            logger.log(Messages.Builder_Result_InvalidArguments());
            throw new AbortException(Messages.Builder_Result_InvalidArguments());
        }

        final FilePath artifactFilePath = new FilePath(workspace, effectiveArtifact);
        if (!artifactFilePath.exists()) {
            logger.log(Messages.Builder_Artifact_NonExist(effectiveArtifact));
            throw new AbortException(Messages.Builder_Artifact_NonExist(effectiveArtifact));
        }

        final String effectiveApiUrl = getEffectiveApiUrl();
        final String effectiveApiKey = getEffectiveApiKey(run);
        logger.log(Messages.Builder_Publishing(effectiveApiUrl));
        final ApiClient apiClient = clientFactory.create(effectiveApiUrl, effectiveApiKey, logger, descriptor.getDependencyTrackConnectionTimeout(), descriptor.getDependencyTrackReadTimeout());
        final UploadResult uploadResult = apiClient.upload(projectId, effectiveProjectName, effectiveProjectVersion,
                artifactFilePath, isEffectiveAutoCreateProjects());

        if (!uploadResult.isSuccess()) {
            throw new AbortException(Messages.Builder_Upload_Failed());
        }

        // add ResultLinkAction even if it may not contain a projectId. but we want to store name version for the future.
        final ResultLinkAction linkAction = new ResultLinkAction(effectiveApiUrl, projectId);
        linkAction.setProjectName(effectiveProjectName);
        linkAction.setProjectVersion(effectiveProjectVersion);
        run.addOrReplaceAction(linkAction);

        logger.log(Messages.Builder_Success(String.format("%s/projects/%s", effectiveApiUrl, projectId != null ? projectId : StringUtils.EMPTY)));

        if (synchronous && StringUtils.isNotBlank(uploadResult.getToken())) {
            publishAnalysisResult(logger, apiClient, uploadResult.getToken(), run, effectiveProjectName, effectiveProjectVersion);
        }
    }

    private void publishAnalysisResult(ConsoleLogger logger, final ApiClient apiClient, final String token, final Run<?, ?> build, final String effectiveProjectName, final String effectiveProjectVersion) throws InterruptedException, ApiClientException, AbortException {
        final long timeout = System.currentTimeMillis() + (60000L * descriptor.getDependencyTrackPollingTimeout());
        final long interval = 1000L * descriptor.getDependencyTrackPollingInterval();
        logger.log(Messages.Builder_Polling());
        Thread.sleep(interval);
        while (apiClient.isTokenBeingProcessed(token)) {
            Thread.sleep(interval);
            if (timeout < System.currentTimeMillis()) {
                logger.log(Messages.Builder_Polling_Timeout_Exceeded());
                // XXX this seems like a fatal error
                throw new AbortException(Messages.Builder_Polling_Timeout_Exceeded());
            }
        }
        if (StringUtils.isBlank(projectId)) {
            // project was auto-created. Fetch it's new uuid so that we can look up the results
            logger.log(Messages.Builder_Project_Lookup(effectiveProjectName, effectiveProjectVersion));
            projectId = apiClient.lookupProject(effectiveProjectName, effectiveProjectVersion).getUuid();
        }
        logger.log(Messages.Builder_Findings_Processing());
        final List<Finding> findings = apiClient.getFindings(projectId);
        final SeverityDistribution severityDistribution = new SeverityDistribution(build.getNumber());
        findings.stream().map(Finding::getVulnerability).map(Vulnerability::getSeverity).forEach(severityDistribution::add);
        final ResultAction projectAction = new ResultAction(findings, severityDistribution);
        projectAction.setDependencyTrackUrl(getEffectiveUrl());
        projectAction.setProjectId(projectId);
        build.addOrReplaceAction(projectAction);

        // update ResultLinkAction with one that surely contains a projectId
        final ResultLinkAction linkAction = new ResultLinkAction(getEffectiveUrl(), projectId);
        linkAction.setProjectName(effectiveProjectName);
        linkAction.setProjectVersion(effectiveProjectVersion);
        build.addOrReplaceAction(linkAction);

        // Get previous results and evaluate to thresholds
        final SeverityDistribution previousSeverityDistribution = Optional.ofNullable(build.getPreviousBuild())
                .map(previousBuild -> previousBuild.getAction(ResultAction.class))
                .map(ResultAction::getSeverityDistribution)
                .orElse(new SeverityDistribution(0));

        evaluateRiskGates(build, logger, severityDistribution, previousSeverityDistribution);
    }

    private void evaluateRiskGates(final Run<?, ?> build, final ConsoleLogger logger, final SeverityDistribution currentDistribution, final SeverityDistribution previousDistribution) throws AbortException {
        final RiskGate riskGate = new RiskGate(getThresholds());
        final Result result = riskGate.evaluate(currentDistribution, previousDistribution);
        if (result.isWorseOrEqualTo(Result.UNSTABLE) && result.isCompleteBuild()) {
            logger.log(Messages.Builder_Threshold_Exceed());
            // allow build to proceed, but mark overall build unstable
            build.setResult(result);
        }
        if (result.isWorseThan(Result.UNSTABLE) && result.isCompleteBuild()) {
            // attempt to halt the build
            throw new AbortException(Messages.Builder_Threshold_Exceed());
        }
    }

    /**
     * @return A Descriptor Implementation
     */
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    /**
     * restore transient fields after deserialization
     *
     * @return this
     * @throws java.io.ObjectStreamException never
     */
    private Object readResolve() throws java.io.ObjectStreamException {
        if (clientFactory == null) {
            clientFactory = ApiClient::new;
        }
        if (descriptor == null) {
            descriptor = getDescriptor();
        }
        overrideGlobals = StringUtils.isNotBlank(dependencyTrackUrl) || StringUtils.isNotBlank(dependencyTrackApiKey) || autoCreateProjects != null;
        return this;
    }

    /**
     * deletes values of optional fields if they are not needed/active before
     * serialization
     *
     * @return this
     * @throws java.io.ObjectStreamException never
     */
    private Object writeReplace() throws java.io.ObjectStreamException {
        if (!overrideGlobals) {
            dependencyTrackUrl = null;
            dependencyTrackApiUrl = null;
            dependencyTrackApiKey = null;
            autoCreateProjects = null;
        }
        if (!isEffectiveAutoCreateProjects()) {
            projectName = null;
            projectVersion = null;
        }
        return this;
    }

    /**
     * @return effective dependencyTrackUrl
     */
    @NonNull
    private String getEffectiveUrl() {
        String url = Optional.ofNullable(PluginUtil.parseBaseUrl(dependencyTrackUrl)).orElse(descriptor.getDependencyTrackUrl());
        return Optional.ofNullable(url).orElse(StringUtils.EMPTY);
    }

    /**
     * @return effective dependencyTrackApiUrl
     */
    @NonNull
    private String getEffectiveApiUrl() {
        String url = Optional.ofNullable(PluginUtil.parseBaseUrl(dependencyTrackApiUrl)).orElse(descriptor.getDependencyTrackApiUrl());
        return Optional.ofNullable(url).orElse(StringUtils.EMPTY);
    }

    /**
     * resolves credential-id to actual api-key
     *
     * @param run needed for credential retrieval
     * @return effective api-key
     */
    @NonNull
    private String getEffectiveApiKey(@NonNull Run<?, ?> run) {
        final String credId = Optional.ofNullable(StringUtils.trimToNull(dependencyTrackApiKey)).orElse(descriptor.getDependencyTrackApiKey());
        if (credId != null) {
            StringCredentials cred = CredentialsProvider.findCredentialById(credId, StringCredentials.class, run);
            // for compatibility reasons when updating from v2.x to 3.0: return original value as is because it may be the api-key itself.
            return Optional.ofNullable(CredentialsProvider.track(run, cred)).map(StringCredentials::getSecret).map(Secret::getPlainText).orElse(credId);
        } else {
            return StringUtils.EMPTY;
        }
    }

    /**
     * @return effective autoCreateProjects
     */
    public boolean isEffectiveAutoCreateProjects() {
        return Optional.ofNullable(autoCreateProjects).orElse(descriptor.isDependencyTrackAutoCreateProjects());
    }
}
