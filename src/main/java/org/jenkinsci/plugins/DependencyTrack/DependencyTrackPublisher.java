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

import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepMonitor;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import java.util.Optional;
import jenkins.tasks.SimpleBuildStep;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.DependencyTrack.model.Finding;
import org.jenkinsci.plugins.DependencyTrack.model.RiskGate;
import org.jenkinsci.plugins.DependencyTrack.model.SeverityDistribution;
import org.jenkinsci.plugins.DependencyTrack.model.UploadResult;
import org.jenkinsci.plugins.DependencyTrack.model.Vulnerability;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

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
     * Specifies an API Key used for authentication.
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
     * @param run a build this is running as a part of
     * @param workspace a workspace to use for any file operations
     * @param env environment variables applicable to this step
     * @param launcher a way to start processes
     * @param listener a place to send output
     * @throws InterruptedException if the step is interrupted
     * @throws IOException if something goes wrong
     */
    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull EnvVars env, @NonNull Launcher launcher, @NonNull TaskListener listener) throws InterruptedException, IOException {
        final ConsoleLogger logger = new ConsoleLogger(listener.getLogger());

        final ApiClient apiClient = clientFactory.create(getEffectiveUrl(), getEffectiveApiKey(), logger, descriptor.getDependencyTrackConnectionTimeout(), descriptor.getDependencyTrackReadTimeout());

        logger.log(Messages.Builder_Publishing() + " - " + getEffectiveUrl());

        if (StringUtils.isBlank(artifact)) {
            logger.log(Messages.Builder_Artifact_Unspecified());
            throw new AbortException(Messages.Builder_Artifact_Unspecified());
        }
        if (StringUtils.isBlank(projectId) && (StringUtils.isBlank(projectName) || StringUtils.isBlank(projectVersion))) {
            logger.log(Messages.Builder_Result_InvalidArguments());
            throw new AbortException(Messages.Builder_Result_InvalidArguments());
        }

        final FilePath artifactFilePath = new FilePath(workspace, artifact);
        if (!artifactFilePath.exists()) {
            logger.log(Messages.Builder_Artifact_NonExist(artifact));
            throw new AbortException(Messages.Builder_Artifact_NonExist(artifact));
        }

        final UploadResult uploadResult = apiClient.upload(projectId, projectName, projectVersion,
                artifactFilePath, isEffectiveAutoCreateProjects());

        if (!uploadResult.isSuccess()) {
            throw new AbortException(Messages.Builder_Upload_Failed());
        }
        
        logger.log(Messages.Builder_Success(String.format("%s/projects/%s", getEffectiveUrl(), projectId != null ? projectId : StringUtils.EMPTY)));

        if (synchronous && StringUtils.isNotBlank(uploadResult.getToken())) {
            publishAnalysisResult(logger, apiClient, uploadResult.getToken(), run);
        }
    }

    private void publishAnalysisResult(ConsoleLogger logger, final ApiClient apiClient, final String token, final Run<?, ?> build) throws InterruptedException, ApiClientException, AbortException {
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
            logger.log(Messages.Builder_Project_Lookup(projectName, projectVersion));
            projectId = apiClient.lookupProject(projectName, projectVersion).getUuid();
        }
        logger.log(Messages.Builder_Findings_Processing());
        final List<Finding> findings = apiClient.getFindings(projectId);
        final SeverityDistribution severityDistribution = new SeverityDistribution(build.getNumber());
        findings.stream().map(Finding::getVulnerability).map(Vulnerability::getSeverity).forEach(severityDistribution::add);
        final ResultAction projectAction = new ResultAction(build, findings, severityDistribution);
        build.addOrReplaceAction(projectAction);

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
     *
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
     * deletes values of optional fields if they are not needed/active before serialization
     * 
     * @return this
     * @throws java.io.ObjectStreamException never
     */
    private Object writeReplace() throws java.io.ObjectStreamException {
        if (!overrideGlobals) {
            dependencyTrackUrl = null;
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
    private String getEffectiveUrl() {
        return Optional.ofNullable(PluginUtil.parseBaseUrl(dependencyTrackUrl)).orElse(descriptor.getDependencyTrackUrl());
    }

    /**
     * @return effective dependencyTrackApiKey
     */
    private String getEffectiveApiKey() {
        return Optional.ofNullable(StringUtils.trimToNull(dependencyTrackApiKey)).orElse(descriptor.getDependencyTrackApiKey());
    }

    /**
     * @return effective autoCreateProjects
     */
    public boolean isEffectiveAutoCreateProjects() {
        return Optional.ofNullable(autoCreateProjects).orElse(descriptor.isDependencyTrackAutoCreateProjects());
    }
}
