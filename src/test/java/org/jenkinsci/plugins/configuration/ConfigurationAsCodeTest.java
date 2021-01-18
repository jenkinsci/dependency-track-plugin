package org.jenkinsci.plugins.configuration;

import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.DependencyTrack.DependencyTrackPublisher;
import org.jenkinsci.plugins.DependencyTrack.DescriptorImpl;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigurationAsCodeTest {

    @Rule
    public JenkinsConfiguredWithCodeRule r = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("dependency_track_test_config.yml")
    public void shouldSupportConfigurationAsCode() throws Exception {
        Jenkins jenkins = Jenkins.get();
        DescriptorImpl descriptor = (DescriptorImpl) jenkins.getDescriptor(DependencyTrackPublisher.class);

        assertThat(descriptor)
                .returns("https://example.org/deptrack", DescriptorImpl::getDependencyTrackUrl)
                .returns("https://api.example.org/deptrack", DescriptorImpl::getDependencyTrackApiUrl)
                .returns("R4nD0m", DescriptorImpl::getDependencyTrackApiKey)
                .returns(false, DescriptorImpl::isDependencyTrackAutoCreateProjects)
                .returns(5, DescriptorImpl::getDependencyTrackPollingTimeout)
                .returns(1, DescriptorImpl::getDependencyTrackPollingInterval)
                .returns(1, DescriptorImpl::getDependencyTrackConnectionTimeout)
                .returns(3, DescriptorImpl::getDependencyTrackReadTimeout)
                ;
    }
}
