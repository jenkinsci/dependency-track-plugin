package org.jenkinsci.plugins.configuration;

import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.jenkinsci.plugins.DependencyTrack.DescriptorImpl;
import org.junit.ClassRule;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import static io.jenkins.plugins.casc.misc.Util.getUnclassifiedRoot;
import static io.jenkins.plugins.casc.misc.Util.toYamlString;
import static org.assertj.core.api.Assertions.assertThat;

public class ConfigurationAsCodeTest {

    @ClassRule
    @ConfiguredWithCode("dependency_track_test_config.yml")
    public static JenkinsConfiguredWithCodeRule r = new JenkinsConfiguredWithCodeRule();

    @Test
    public void shouldSupportConfigurationAsCode() throws Exception {
        DescriptorImpl descriptor = r.jenkins.getDescriptorByType(DescriptorImpl.class);

        assertThat(descriptor)
                .returns("https://example.org/deptrack", DescriptorImpl::getDependencyTrackUrl)
                .returns("https://ui.example.org", DescriptorImpl::getDependencyTrackFrontendUrl)
                .returns("R4nD0m", DescriptorImpl::getDependencyTrackApiKey)
                .returns(false, DescriptorImpl::isDependencyTrackAutoCreateProjects)
                .returns(5, DescriptorImpl::getDependencyTrackPollingTimeout)
                .returns(1, DescriptorImpl::getDependencyTrackPollingInterval)
                .returns(1, DescriptorImpl::getDependencyTrackConnectionTimeout)
                .returns(3, DescriptorImpl::getDependencyTrackReadTimeout)
                ;
    }

    @Test
    public void shouldSupportConfigurationExport() throws Exception {
        var registry = ConfiguratorRegistry.get();
        var context = new ConfigurationContext(registry);
        var yourAttribute = getUnclassifiedRoot(context).get("dependencyTrackPublisher");

        var exported = toYamlString(yourAttribute);

        try (var res = getClass().getClassLoader().getResourceAsStream("dependency_track_test_config_exported.yml")) {
            var expected = new String(res.readAllBytes());
            assertThat(exported).isEqualTo(expected);
        }
    }
}
