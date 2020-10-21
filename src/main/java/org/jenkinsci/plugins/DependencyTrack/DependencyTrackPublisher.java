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
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepMonitor;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.tasks.SimpleBuildStep;
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
public class DependencyTrackPublisher extends ThresholdCapablePublisher implements SimpleBuildStep, Serializable {

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
    
    private transient final ApiClientFactory clientFactory;
    
    // Fields in config.jelly must match the parameter names
    @DataBoundConstructor
    public DependencyTrackPublisher(final String artifact, final boolean synchronous) {
        this(artifact, synchronous, ApiClient::new);
    }
    
    DependencyTrackPublisher(String artifact, boolean synchronous, @lombok.NonNull ApiClientFactory clientFactory) {
        this.artifact = artifact;
        this.synchronous = synchronous;
        this.clientFactory = clientFactory;
    }

    /**
     * This method is called whenever the build step is executed.
     *
     * @param build A Run object
     * @param filePath A FilePath object
     * @param launcher A Launcher object
     * @param listener A BuildListener object
     * @throws java.lang.InterruptedException
     * @throws java.io.IOException
     */
    @Override
    public void perform(@NonNull final Run<?, ?> build,
            @NonNull final FilePath filePath,
            @NonNull final Launcher launcher,
            @NonNull final TaskListener listener) throws InterruptedException, IOException {

        final ConsoleLogger logger = new ConsoleLogger(listener);
        final DescriptorImpl descriptor = getDescriptor();

        final ApiClient apiClient = clientFactory.create(descriptor.getDependencyTrackUrl(), descriptor.getDependencyTrackApiKey(), logger);

        logger.log(Messages.Builder_Publishing() + " - " + descriptor.getDependencyTrackUrl());

        final boolean autoCreateProject = descriptor.isDependencyTrackAutoCreateProjects();

        if (StringUtils.isBlank(artifact)) {
            logger.log(Messages.Builder_Artifact_Unspecified());
            throw new AbortException("Artifact not specified");
        }
        if (StringUtils.isBlank(projectId) && (StringUtils.isBlank(projectName) || StringUtils.isBlank(projectVersion))) {
            logger.log(Messages.Builder_Result_InvalidArguments());
            throw new AbortException("Invalid arguments");
        }

        final FilePath artifactFilePath = new FilePath(filePath, artifact);
        if (!artifactFilePath.exists()) {
            logger.log(Messages.Builder_Artifact_NonExist());
            throw new AbortException("Nonexistent artifact " + artifact);
        }

        final UploadResult uploadResult = apiClient.upload(projectId, projectName, projectVersion,
                artifactFilePath, autoCreateProject);

        if (!uploadResult.isSuccess()) {
            throw new AbortException("Dependency Track server upload failed");
        }

        if (synchronous && StringUtils.isNotBlank(uploadResult.getToken())) {
            publishAnalysisResult(logger, apiClient, uploadResult.getToken(), build);
        }
    }

    private void publishAnalysisResult(ConsoleLogger logger, final ApiClient apiClient, final String token, final Run<?, ?> build) throws InterruptedException, ApiClientException, AbortException {
        final long timeout = System.currentTimeMillis() + (60000L * getDescriptor().getDependencyTrackPollingTimeout());
        Thread.sleep(10000);
        logger.log(Messages.Builder_Polling());
        while (apiClient.isTokenBeingProcessed(token)) {
            Thread.sleep(10000);
            if (timeout < System.currentTimeMillis()) {
                logger.log(Messages.Builder_Polling_Timeout_Exceeded());
                // XXX this seems like a fatal error
                throw new AbortException("Dependency Track server response timeout");
            }
            logger.log(Messages.Builder_Polling());
        }
        if (StringUtils.isBlank(projectId)) {
            // project was auto-created. Fetch it's new uuid so that we can look up the results
            logger.log(Messages.Builder_Project_Lookup());
            projectId = apiClient.lookupProject(projectName, projectVersion).getUuid();
        }
        logger.log(Messages.Builder_Findings_Processing());
        final List<Finding> findings = apiClient.getFindings(projectId);
        final SeverityDistribution severityDistribution = new SeverityDistribution(build.getNumber());
        findings.stream().map(Finding::getVulnerability).map(Vulnerability::getSeverity).forEach(severityDistribution::add);
        final ResultAction projectAction = new ResultAction(build, findings, severityDistribution);
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
                final Result result = riskGate.evaluate(severityDistribution, Collections.emptyList(), severityDistribution, findings);
                evaluateRiskGates(build, logger, result);
            }
        } else { // Resolves https://issues.jenkins-ci.org/browse/JENKINS-58387
            final Result result = riskGate.evaluate(severityDistribution, Collections.emptyList(), severityDistribution, findings);
            evaluateRiskGates(build, logger, result);
        }
    }

    private void evaluateRiskGates(final Run<?, ?> build, final ConsoleLogger logger, final Result result) throws AbortException {
        if (Result.SUCCESS != result) {
            logger.log(Messages.Builder_Threshold_Exceed());
            if (Result.FAILURE == result) {
                // attempt to halt the build
                throw new AbortException("Severity distribution failure thresholds exceeded");
            } else {
                // allow build to proceed, but mark overall build unstable
                build.setResult(result);
            }
        }
    }

    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
        return new JobAction(project);
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

}
