/*
 * Copyright 2021 OWASP.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jenkinsci.plugins.DependencyTrack;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.security.AccessDeniedException3;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import net.sf.json.JSONArray;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Files;
import org.jenkinsci.plugins.DependencyTrack.model.Finding;
import org.jenkinsci.plugins.DependencyTrack.model.PolicyViolation;
import org.jenkinsci.plugins.DependencyTrack.model.SeverityDistribution;
import org.jenkinsci.plugins.DependencyTrack.model.ViolationDistribution;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
public class ResultActionTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private List<Finding> getTestFindings() {
        File findings = new File("src/test/resources/findings.json");
        return FindingParser.parse(Files.contentOf(findings, StandardCharsets.UTF_8));
    }

    private List<PolicyViolation> getTestPolicyViolations()
    {
        final File PolicyViolations = new File("src/test/resources/policyViolations.json");
        return PolicyViolationsParser.parse(Files.contentOf(PolicyViolations, StandardCharsets.UTF_8));
    }

    @Test
    public void getVersionHashTest() {
        final ResultAction uut = new ResultAction(null, null, null, null);
        // does not equal sha-256 of empty string
        assertThat(uut.getVersionHash()).matches("[a-f0-9]{64}").isNotEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    public void getFindingsJsonPermissionTest() throws IOException {
        final MockAuthorizationStrategy mockAuthorizationStrategy = new MockAuthorizationStrategy();
        j.jenkins.setAuthorizationStrategy(mockAuthorizationStrategy);
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());

        final FreeStyleProject project = j.createFreeStyleProject();
        final FreeStyleBuild b1 = new FreeStyleBuild(project);
        final ResultAction uut = new ResultAction(getTestFindings(), new SeverityDistribution(1), null, new ViolationDistribution(1));
        uut.onLoad(b1);

        final User anonymous = User.getOrCreateByIdOrFullName(ACL.ANONYMOUS_USERNAME);
        // without propper permissions
        try (ACLContext ignored = ACL.as(anonymous)) {
            assertThatThrownBy(() -> uut.getFindingsJson()).isInstanceOf(AccessDeniedException3.class);
        }
        // with propper permissions
        try (ACLContext ignored = ACL.as(anonymous)) {
            mockAuthorizationStrategy.grant(Job.READ).onItems(project).to(anonymous);
            assertThatCode(() -> uut.getFindingsJson()).doesNotThrowAnyException();
        }
    }

    @Test
    public void getFindingsJson() throws IOException {
        final FreeStyleProject project = j.createFreeStyleProject();
        final FreeStyleBuild b1 = new FreeStyleBuild(project);
        final ResultAction uut = new ResultAction(getTestFindings(), new SeverityDistribution(1), null, new ViolationDistribution(1));
        uut.onLoad(b1);
        Assertions.<JSONArray>assertThat(uut.getFindingsJson()).isEqualTo(JSONArray.fromObject(getTestFindings()));
    }

    @Test
    public void getPolicyViolationsJson() throws IOException {
        final FreeStyleProject project = j.createFreeStyleProject();
        final FreeStyleBuild b1 = new FreeStyleBuild(project);
        final ResultAction uut = new ResultAction(null, new SeverityDistribution(1), getTestPolicyViolations(), new ViolationDistribution(1));
        uut.onLoad(b1);
        Assertions.<JSONArray>assertThat(uut.getPolicyViolationsJson()).isEqualTo(JSONArray.fromObject(getTestPolicyViolations()));
    }

    @Test
    public void hasFindingsTest() {
        assertThat(new ResultAction(null, new SeverityDistribution(1), null, new ViolationDistribution(1)).hasFindings()).isFalse();
        assertThat(new ResultAction(Collections.emptyList(), new SeverityDistribution(1), null, new ViolationDistribution(1)).hasFindings()).isFalse();
        assertThat(new ResultAction(getTestFindings(), new SeverityDistribution(1), null, new ViolationDistribution(1)).hasFindings()).isTrue();
    }

    @Test
    public void hasPolicyViolationsTest() {
        assertThat(new ResultAction(null, new SeverityDistribution(1), null, new ViolationDistribution(1)).hasFindings()).isFalse();
        assertThat(new ResultAction(null, new SeverityDistribution(1), Collections.emptyList(), new ViolationDistribution(1)).hasPolicyViolations()).isFalse();
        assertThat(new ResultAction(null, new SeverityDistribution(1), getTestPolicyViolations(), new ViolationDistribution(1)).hasPolicyViolations()).isTrue();
    }
}
