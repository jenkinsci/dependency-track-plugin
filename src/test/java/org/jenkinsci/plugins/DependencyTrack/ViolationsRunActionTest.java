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
import java.util.List;
import net.sf.json.JSONArray;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Files;
import org.jenkinsci.plugins.DependencyTrack.model.Violation;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
@WithJenkins
class ViolationsRunActionTest {

    private List<Violation> getTestViolations() {
        File violations = new File("src/test/resources/violations.json");
        return ViolationParser.parse(Files.contentOf(violations, StandardCharsets.UTF_8));
    }

    @Test
    void getVersionHashTest(JenkinsRule j) {
        final ViolationsRunAction uut = new ViolationsRunAction(null);
        // does not equal sha-256 of empty string
        assertThat(uut.getVersionHash()).describedAs("hashed Version should not be empty").matches("[a-f0-9]{64}").isNotEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    void getViolationsJsonPermissionTest(JenkinsRule j) throws IOException {
        final MockAuthorizationStrategy mockAuthorizationStrategy = new MockAuthorizationStrategy();
        j.jenkins.setAuthorizationStrategy(mockAuthorizationStrategy);
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());

        FreeStyleProject project;
        try (ACLContext ignored = ACL.as(User.getOrCreateByIdOrFullName(ACL.SYSTEM_USERNAME))) {
            mockAuthorizationStrategy.grant(Job.CREATE).onRoot().to(ACL.SYSTEM_USERNAME);
            project = j.createFreeStyleProject();
        }
        final FreeStyleBuild b1 = new FreeStyleBuild(project);
        final ViolationsRunAction uut = new ViolationsRunAction(getTestViolations());
        uut.onLoad(b1);

        final User anonymous = User.getOrCreateByIdOrFullName(ACL.ANONYMOUS_USERNAME);
        // without propper permissions
        try (ACLContext ignored = ACL.as(anonymous)) {
            assertThatThrownBy(uut::getViolationsJson).isInstanceOf(AccessDeniedException3.class);
        }
        // with propper permissions
        try (ACLContext ignored = ACL.as(anonymous)) {
            mockAuthorizationStrategy.grant(Job.READ).onItems(project).to(anonymous);
            assertThatCode(uut::getViolationsJson).doesNotThrowAnyException();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void getViolationsJson(JenkinsRule j) throws IOException {
        final FreeStyleProject project = j.createFreeStyleProject();
        final FreeStyleBuild b1 = new FreeStyleBuild(project);
        final List<Violation> violations = getTestViolations();
        final ViolationsRunAction uut = new ViolationsRunAction(violations);
        uut.onLoad(b1);
        Assertions.<JSONArray>assertThat(uut.getViolationsJson()).isEqualTo(JSONArray.fromObject(violations));
    }

    @Test
    void hasViolationsTest() {
        assertThat(new ViolationsRunAction(null).hasViolations()).isFalse();
        assertThat(new ViolationsRunAction(List.of()).hasViolations()).isFalse();
        assertThat(new ViolationsRunAction(getTestViolations()).hasViolations()).isTrue();
    }
}
