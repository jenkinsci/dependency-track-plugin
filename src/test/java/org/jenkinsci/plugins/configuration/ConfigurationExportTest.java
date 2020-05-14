package org.jenkinsci.plugins.configuration;

import io.jenkins.plugins.casc.ConfigurationAsCode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.LoggerRule;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.logging.Level;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.jvnet.hudson.test.LoggerRule.recorded;

public class ConfigurationExportTest {

    public final JenkinsRule j = new JenkinsRule();

    private final LoggerRule logRule = new LoggerRule().record("io.jenkins.plugins.casc", Level.INFO).capture(1000);

    @Rule
    public RuleChain chain = RuleChain.outerRule(j).around(logRule);


    @Test
    public void testConfigurationExport() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ConfigurationAsCode.get().export(outputStream);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        String configuration = new Yaml().load(inputStream).toString();

        assertThat(configuration, containsString("dependencyTrackAutoCreateProjects"));
        assertThat(configuration, not(containsString("FAILED TO EXPORT")));
        assertThat(logRule, not(recorded(containsString("Configuration-as-Code can't handle type"))));
    }
}
