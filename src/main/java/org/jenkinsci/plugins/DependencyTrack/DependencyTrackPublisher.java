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
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.EnvVars;
import hudson.tasks.Recorder;
import hudson.util.Secret;
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
import org.jenkinsci.plugins.DependencyTrack.model.Thresholds;
import org.jenkinsci.plugins.DependencyTrack.model.UploadResult;
import org.jenkinsci.plugins.DependencyTrack.model.Violation;
import org.jenkinsci.plugins.DependencyTrack.model.ViolationState;
import org.jenkinsci.plugins.DependencyTrack.model.Vulnerability;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import static org.jenkinsci.plugins.DependencyTrack.model.Permissions.VIEW_POLICY_VIOLATION;

@Getter
@Setter(onMethod_ = {@DataBoundSetter})
@EqualsAndHashCode(callSuper = true)
public final class DependencyTrackPublisher extends Recorder implements SimpleBuildStep, Serializable {

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
     * Additional project properties to set
     */
    private ProjectProperties projectProperties;
    
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
     * Specifies the alternative base URL to the frontend of Dependency-Track v3
     * or higher.
     */
    private String dependencyTrackFrontendUrl;

    /**
     * Specifies the credential-id for an API Key used for authentication.
     */
    private String dependencyTrackApiKey;

    /**
     * Specifies the maximum number of minutes to wait for synchronous jobs to
     * complete.
     */
    private Integer dependencyTrackPollingTimeout;

    /**
     * Defines the number of seconds to wait between two checks for
     * Dependency-Track to process a job (Synchronous Publishing Mode).
     */
    private Integer dependencyTrackPollingInterval;

    /**
     * the connection-timeout in seconds for every call to DT
     */
    private Integer dependencyTrackConnectionTimeout;

    /**
     * the read-timeout in seconds for every call to DT
     */
    private Integer dependencyTrackReadTimeout;

    /**
     * Specifies whether the API key provided has the PROJECT_CREATION_UPLOAD
     * permission.
     */
    private Boolean autoCreateProjects;

    /**
     * Threshold level for total number of critical findings for job status
     * UNSTABLE
     */
    private Integer unstableTotalCritical;

    /**
     * Threshold level for total number of high findings for job status UNSTABLE
     */
    private Integer unstableTotalHigh;

    /**
     * Threshold level for total number of medium findings for job status
     * UNSTABLE
     */
    private Integer unstableTotalMedium;

    /**
     * Threshold level for total number of low findings for job status UNSTABLE
     */
    private Integer unstableTotalLow;

    /**
     * Threshold level for total number of unassigned findings for job status UNSTABLE
     */
    private Integer unstableTotalUnassigned;

    /**
     * Threshold level for total number of critical findings for job status
     * FAILED
     */
    private Integer failedTotalCritical;

    /**
     * Threshold level for total number of high findings for job status FAILED
     */
    private Integer failedTotalHigh;

    /**
     * Threshold level for total number of medium findings for job status FAILED
     */
    private Integer failedTotalMedium;

    /**
     * Threshold level for total number of low findings for job status FAILED
     */
    private Integer failedTotalLow;

    /**
     * Threshold level for total number of unassigned findings for job status FAILED
     */
    private Integer failedTotalUnassigned;

    /**
     * Threshold level for number of new critical findings for job status
     * UNSTABLE
     */
    private Integer unstableNewCritical;

    /**
     * Threshold level for number of new high findings for job status UNSTABLE
     */
    private Integer unstableNewHigh;

    /**
     * Threshold level for number of new medium findings for job status UNSTABLE
     */
    private Integer unstableNewMedium;

    /**
     * Threshold level for number of new low findings for job status UNSTABLE
     */
    private Integer unstableNewLow;

    /**
     * Threshold level for number of new unassigned findings for job status UNSTABLE
     */
    private Integer unstableNewUnassigned;

    /**
     * Threshold level for number of new critical findings for job status FAILED
     */
    private Integer failedNewCritical;

    /**
     * Threshold level for number of new high findings for job status FAILED
     */
    private Integer failedNewHigh;

    /**
     * Threshold level for number of new medium findings for job status FAILED
     */
    private Integer failedNewMedium;

    /**
     * Threshold level for number of new low findings for job status FAILED
     */
    private Integer failedNewLow;

    /**
     * Threshold level for number of new unassigned findings for job status FAILED
     */
    private Integer failedNewUnassigned;

    /**
     * mark job as UNSTABLE if there is at least one policy violation of severity
     * warning
     */
    private boolean warnOnViolationWarn;

    /**
     * mark job as FAILED if there is at least one policy violation of severity
     * fail
     */
    private boolean failOnViolationFail;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private transient ApiClientFactory clientFactory;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private transient DescriptorImpl descriptor;

    private transient boolean overrideGlobals;
    
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private transient String projectIdCache;

    // Fields in config.jelly must match the parameter names
    @DataBoundConstructor
    public DependencyTrackPublisher(final String artifact, final boolean synchronous) {
        this(artifact, synchronous, ApiClient::new);
    }

    DependencyTrackPublisher(final String artifact, final boolean synchronous, @lombok.NonNull final ApiClientFactory clientFactory) {
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
    public void perform(@NonNull final Run<?, ?> run, @NonNull final FilePath workspace, @NonNull final EnvVars env, @NonNull final Launcher launcher, @NonNull final TaskListener listener) throws InterruptedException, IOException {
        final ConsoleLogger logger = new ConsoleLogger(listener.getLogger());
        final String effectiveProjectName = env.expand(projectName);
        final String effectiveProjectVersion = env.expand(projectVersion);
        final String effectiveArtifact = env.expand(artifact);
        final boolean effectiveAutocreate = isEffectiveAutoCreateProjects();
        projectIdCache = null;

        if (StringUtils.isBlank(effectiveArtifact)) {
            logger.log(Messages.Builder_Artifact_Unspecified());
            throw new AbortException(Messages.Builder_Artifact_Unspecified());
        }
        if (StringUtils.isBlank(projectId) && (StringUtils.isBlank(effectiveProjectName) || StringUtils.isBlank(effectiveProjectVersion))) {
            logger.log(Messages.Builder_Result_InvalidArguments());
            throw new AbortException(Messages.Builder_Result_InvalidArguments());
        }
        if (StringUtils.isBlank(projectId) && !effectiveAutocreate) {
            logger.log(Messages.Builder_Result_ProjectIdMissing());
            throw new AbortException(Messages.Builder_Result_ProjectIdMissing());
        }

        final FilePath artifactFilePath = new FilePath(workspace, effectiveArtifact);
        if (!artifactFilePath.exists()) {
            logger.log(Messages.Builder_Artifact_NonExist(effectiveArtifact));
            throw new AbortException(Messages.Builder_Artifact_NonExist(effectiveArtifact));
        }

        final String effectiveUrl = getEffectiveUrl();
        final String effectiveApiKey = getEffectiveApiKey(run);
        final ProjectProperties effectiveProjectProperties = expandProjectProperties(env);
        logger.log(Messages.Builder_Publishing(effectiveUrl, effectiveArtifact));
        final ApiClient apiClient = clientFactory.create(effectiveUrl, effectiveApiKey, logger, getEffectiveConnectionTimeout(), getEffectiveReadTimeout());
        final UploadResult uploadResult = apiClient.upload(projectId, effectiveProjectName, effectiveProjectVersion,
                artifactFilePath, effectiveAutocreate, effectiveProjectProperties);

        if (!uploadResult.isSuccess()) {
            throw new AbortException(Messages.Builder_Upload_Failed());
        }

        // add ResultLinkAction even if it may not contain a projectId. but we want to store name and version for the future.
        final ResultLinkAction linkAction = new ResultLinkAction(getEffectiveFrontendUrl(), projectId);
        linkAction.setProjectName(effectiveProjectName);
        linkAction.setProjectVersion(effectiveProjectVersion);
        run.addOrReplaceAction(linkAction);

        logger.log(Messages.Builder_Success(String.format("%s/projects/%s", getEffectiveFrontendUrl(), StringUtils.isNotBlank(projectId) ? projectId : StringUtils.EMPTY)));
        
        updateProjectProperties(logger, apiClient, effectiveProjectName, effectiveProjectVersion, effectiveProjectProperties);

        final var thresholds = getThresholds();
        if (synchronous && StringUtils.isNotBlank(uploadResult.getToken())) {
            final var resultActions = publishAnalysisResult(logger, apiClient, uploadResult.getToken(), run, effectiveProjectName, effectiveProjectVersion);
            if (thresholds.hasValues()) {
                final var resultAction = resultActions.findingsAction;
                evaluateRiskGates(run, logger, resultAction.getSeverityDistribution(), thresholds);
            }
            if (resultActions.violationsAction != null) {
                final var violationsAction = resultActions.violationsAction;
                evaluateViolations(run, logger, violationsAction.getViolations());
            }
        }
        if (!synchronous && thresholds.hasValues()) {
            logger.log(Messages.Builder_Threshold_NoSync());
        }
    }

    private PublishAnalysisResult publishAnalysisResult(final ConsoleLogger logger, final ApiClient apiClient, final String token, final Run<?, ?> build, final String effectiveProjectName, final String effectiveProjectVersion) throws InterruptedException, ApiClientException, AbortException {
        final long timeout = System.currentTimeMillis() + (60000L * getEffectivePollingTimeout());
        final long interval = 1000L * getEffectivePollingInterval();
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
        final String effectiveProjectId = lookupProjectId(logger, apiClient, effectiveProjectName, effectiveProjectVersion);
        logger.log(Messages.Builder_Findings_Processing());
        final List<Finding> findings = apiClient.getFindings(effectiveProjectId);
        final SeverityDistribution severityDistribution = new SeverityDistribution(build.getNumber());
        findings.stream().map(Finding::getVulnerability).map(Vulnerability::getSeverity).forEach(severityDistribution::add);
        final var findingsAction = new ResultAction(findings, severityDistribution);
        findingsAction.setDependencyTrackUrl(getEffectiveFrontendUrl());
        findingsAction.setProjectId(effectiveProjectId);
        build.addOrReplaceAction(findingsAction);

        final var team = apiClient.getTeamPermissions();
        ViolationsRunAction violationsAction = null;
        // for compatibility reasons: the permission may not be present so we check if it is. otherwise an exception would be thrown.
        if (team.getPermissions().contains(VIEW_POLICY_VIOLATION.toString())) {
            logger.log(Messages.Builder_Violations_Processing());
            final var violations = apiClient.getViolations(effectiveProjectId);
            violationsAction = new ViolationsRunAction(violations);
            violationsAction.setDependencyTrackUrl(getEffectiveFrontendUrl());
            violationsAction.setProjectId(effectiveProjectId);
            build.addOrReplaceAction(violationsAction);
        } else {
            logger.log(Messages.Builder_Violations_Skipped(VIEW_POLICY_VIOLATION, team.getName()));
        }

        // update ResultLinkAction with one that surely contains a projectId
        final ResultLinkAction linkAction = new ResultLinkAction(getEffectiveFrontendUrl(), effectiveProjectId);
        linkAction.setProjectName(effectiveProjectName);
        linkAction.setProjectVersion(effectiveProjectVersion);
        build.addOrReplaceAction(linkAction);

        return new PublishAnalysisResult(findingsAction, violationsAction);
    }

    private void evaluateRiskGates(final Run<?, ?> build, final ConsoleLogger logger, final SeverityDistribution currentDistribution, final Thresholds thresholds) throws AbortException {
        // Get previous results and evaluate to thresholds
        final SeverityDistribution previousDistribution = Optional.ofNullable(getPreviousBuildWithAnalysisResult(build))
                .map(previousBuild -> previousBuild.getAction(ResultAction.class))
                .map(ResultAction::getSeverityDistribution)
                .orElse(null);
        if (previousDistribution != null) {
            logger.log(Messages.Builder_Threshold_ComparingTo(previousDistribution.getBuildNumber()));
        } else {
            logger.log(Messages.Builder_Threshold_NoComparison());
        }
        final RiskGate riskGate = new RiskGate(thresholds);
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

    private void evaluateViolations(final Run<?, ?> build, final ConsoleLogger logger, final List<Violation> violations) throws AbortException {
        if (warnOnViolationWarn && violations.stream().anyMatch(violation -> violation.getState() == ViolationState.WARN)) {
            logger.log(Messages.Builder_Violations_Exceed());
            build.setResult(Result.UNSTABLE);
        }
        if (failOnViolationFail && violations.stream().anyMatch(violation -> violation.getState() == ViolationState.FAIL)) {
            throw new AbortException(Messages.Builder_Violations_Exceed());
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
        overrideGlobals = StringUtils.isNotBlank(dependencyTrackUrl) || StringUtils.isNotBlank(dependencyTrackFrontendUrl) || StringUtils.isNotBlank(dependencyTrackApiKey) || autoCreateProjects != null;
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
            dependencyTrackFrontendUrl = null;
            dependencyTrackApiKey = null;
            autoCreateProjects = null;
            dependencyTrackPollingTimeout = null;
            dependencyTrackPollingInterval = null;
            dependencyTrackConnectionTimeout = null;
            dependencyTrackReadTimeout = null;
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
        String url = Optional.ofNullable(PluginUtil.parseBaseUrl(dependencyTrackUrl)).orElseGet(descriptor::getDependencyTrackUrl);
        return Optional.ofNullable(url).orElse(StringUtils.EMPTY);
    }

    /**
     * @return effective dependencyTrackFrontendUrl
     */
    @NonNull
    private String getEffectiveFrontendUrl() {
        String url = Optional.ofNullable(PluginUtil.parseBaseUrl(dependencyTrackFrontendUrl)).orElseGet(descriptor::getDependencyTrackFrontendUrl);
        return Optional.ofNullable(url).orElseGet(this::getEffectiveUrl);
    }

    /**
     * resolves credential-id to actual api-key
     *
     * @param run needed for credential retrieval
     * @return effective api-key
     */
    @NonNull
    private String getEffectiveApiKey(final @NonNull Run<?, ?> run) {
        final String credId = Optional.ofNullable(StringUtils.trimToNull(dependencyTrackApiKey)).orElseGet(descriptor::getDependencyTrackApiKey);
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
        return Optional.ofNullable(autoCreateProjects).orElseGet(descriptor::isDependencyTrackAutoCreateProjects);
    }

    /**
     * @return effective dependencyTrackPollingTimeout
     */
    @NonNull
    private int getEffectivePollingTimeout() {
        return Optional.ofNullable(dependencyTrackPollingTimeout).filter(v -> v > 0).orElseGet(descriptor::getDependencyTrackPollingTimeout);
    }

    /**
     * @return effective dependencyTrackPollingInterval
     */
    @NonNull
    private int getEffectivePollingInterval() {
        return Optional.ofNullable(dependencyTrackPollingInterval).filter(v -> v > 0).orElseGet(descriptor::getDependencyTrackPollingInterval);
    }

    /**
     * @return effective dependencyTrackConnectionTimeout
     */
    @NonNull
    private int getEffectiveConnectionTimeout() {
        return Optional.ofNullable(dependencyTrackConnectionTimeout).filter(v -> v >= 0).orElseGet(descriptor::getDependencyTrackConnectionTimeout);
    }

    /**
     * @return effective dependencyTrackReadTimeout
     */
    @NonNull
    private int getEffectiveReadTimeout() {
        return Optional.ofNullable(dependencyTrackReadTimeout).filter(v -> v >= 0).orElseGet(descriptor::getDependencyTrackReadTimeout);
    }

    /**
     * Returns the last build that was actually built and has an analysis result ({@link ResultAction}) 
     * @param run the build from where to start (the one running now)
     * @return the last build that was actually built and has an analysis result, or {@code null} if none was found
     */
    @Nullable
    private Run<?, ?> getPreviousBuildWithAnalysisResult(final @NonNull Run<?, ?> run) {
        Run<?, ?> r = run.getPreviousSuccessfulBuild();
        while (r != null && (r.getResult() == null || r.getResult() == Result.NOT_BUILT || r.getAction(ResultAction.class) == null)) {
            r = r.getPreviousSuccessfulBuild();
        }
        return r;
    }

    @NonNull
    private Thresholds getThresholds() {
        final Thresholds thresholds = new Thresholds();
        thresholds.totalFindings.unstableCritical = unstableTotalCritical;
        thresholds.totalFindings.unstableHigh = unstableTotalHigh;
        thresholds.totalFindings.unstableMedium = unstableTotalMedium;
        thresholds.totalFindings.unstableLow = unstableTotalLow;
        thresholds.totalFindings.unstableUnassigned = unstableTotalUnassigned;
        thresholds.totalFindings.failedCritical = failedTotalCritical;
        thresholds.totalFindings.failedHigh = failedTotalHigh;
        thresholds.totalFindings.failedMedium = failedTotalMedium;
        thresholds.totalFindings.failedLow = failedTotalLow;
        thresholds.totalFindings.failedUnassigned = failedTotalUnassigned;

        thresholds.newFindings.unstableCritical = unstableNewCritical;
        thresholds.newFindings.unstableHigh = unstableNewHigh;
        thresholds.newFindings.unstableMedium = unstableNewMedium;
        thresholds.newFindings.unstableLow = unstableNewLow;
        thresholds.newFindings.unstableUnassigned = unstableNewUnassigned;
        thresholds.newFindings.failedCritical = failedNewCritical;
        thresholds.newFindings.failedHigh = failedNewHigh;
        thresholds.newFindings.failedMedium = failedNewMedium;
        thresholds.newFindings.failedLow = failedNewLow;
        thresholds.newFindings.failedUnassigned = failedNewUnassigned;
        return thresholds;
    }
    
    private void updateProjectProperties(final ConsoleLogger logger, final ApiClient apiClient, final String effectiveProjectName, final String effectiveProjectVersion, final ProjectProperties effectiveProjectProperties) throws ApiClientException {
        // check whether there are settings other than those of the parent project.
        // the parent project is set during upload.
        boolean doUpdateProject = projectProperties != null && ( // noformat
                projectProperties.getDescription() != null
                || projectProperties.getGroup() != null
                || projectProperties.getSwidTagId() != null
                || !projectProperties.getTags().isEmpty());

        if (doUpdateProject) {
            logger.log(Messages.Builder_Project_Update());
            final String id = lookupProjectId(logger, apiClient, effectiveProjectName, effectiveProjectVersion);
            apiClient.updateProjectProperties(id, effectiveProjectProperties);
        }
    }
    
    private String lookupProjectId(final ConsoleLogger logger, final ApiClient apiClient, final String effectiveProjectName, final String effectiveProjectVersion) throws ApiClientException {
        if (StringUtils.isBlank(projectId)) {
            if (StringUtils.isBlank(projectIdCache)) {
                logger.log(Messages.Builder_Project_Lookup(effectiveProjectName, effectiveProjectVersion));
                projectIdCache = apiClient.lookupProject(effectiveProjectName, effectiveProjectVersion).getUuid();
            }
        } else {
            projectIdCache = projectId;
        }
        return projectIdCache;
    }

    private ProjectProperties expandProjectProperties(final EnvVars env) {
        if (projectProperties != null) {
            final var expandedProperties = new ProjectProperties();
            Optional.ofNullable(projectProperties.getDescription()).map(env::expand).ifPresent(expandedProperties::setDescription);
            Optional.ofNullable(projectProperties.getGroup()).map(env::expand).ifPresent(expandedProperties::setGroup);
            Optional.ofNullable(projectProperties.getParentId()).map(env::expand).ifPresent(expandedProperties::setParentId);
            Optional.ofNullable(projectProperties.getParentName()).map(env::expand).ifPresent(expandedProperties::setParentName);
            Optional.ofNullable(projectProperties.getParentVersion()).map(env::expand).ifPresent(expandedProperties::setParentVersion);
            Optional.ofNullable(projectProperties.getSwidTagId()).map(env::expand).ifPresent(expandedProperties::setSwidTagId);
            expandedProperties.setTags(projectProperties.getTags().stream().map(env::expand).toList());
            return expandedProperties;
        }
        return null;
    }

    private static record PublishAnalysisResult(@NonNull ResultAction findingsAction, @Nullable ViolationsRunAction violationsAction) {} 
}
