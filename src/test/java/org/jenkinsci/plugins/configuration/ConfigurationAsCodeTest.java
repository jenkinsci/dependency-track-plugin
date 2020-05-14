package org.jenkinsci.plugins.configuration;

import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.DependencyTrack.DependencyTrackPublisher;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


public class ConfigurationAsCodeTest {

    @Rule
    public JenkinsConfiguredWithCodeRule r = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("dependency_track_test_config.yml")
    public void shouldSupportConfigurationAsCode() throws Exception {
        Jenkins jenkins = Jenkins.get();
        DependencyTrackPublisher.DescriptorImpl descriptor =
            (DependencyTrackPublisher.DescriptorImpl) jenkins.getDescriptor(DependencyTrackPublisher.class);

        assertEquals("https://example.org/deptrack", descriptor.getDependencyTrackUrl());
        assertEquals("R4nD0m", descriptor.getDependencyTrackApiKey());
        assertFalse(descriptor.isDependencyTrackAutoCreateProjects());
        assertEquals(5, descriptor.getDependencyTrackPollingTimeout());
    }
}