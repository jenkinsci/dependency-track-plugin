/*
 * Copyright 2024 OWASP.
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
import hudson.model.Run;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.security.AccessDeniedException3;
import hudson.util.RunList;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import net.sf.json.JSONArray;
import org.jenkinsci.plugins.DependencyTrack.model.Violation;
import org.jenkinsci.plugins.DependencyTrack.model.ViolationState;
import org.jenkinsci.plugins.DependencyTrack.model.ViolationType;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatObject;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
@WithJenkins
class ViolationsJobActionTest {

    @Test
    void isTrendVisible() {
        Job job = mock(Job.class);
        Run run = mock(Run.class);
        when(run.getAction(ViolationsRunAction.class)).thenReturn(new ViolationsRunAction(List.of()));
        when(job.getBuilds())
                .thenReturn(RunList.fromRuns(List.of()))
                .thenReturn(RunList.fromRuns(List.of(run)));
        var uut = new ViolationsJobAction(job);
        assertThat(uut.isTrendVisible()).isFalse();
        assertThat(uut.isTrendVisible()).isTrue();
    }

    @Test
    void getViolationsTrendPermissionTest(JenkinsRule j) throws IOException {
        final var mockAuthorizationStrategy = new MockAuthorizationStrategy();
        j.jenkins.setAuthorizationStrategy(mockAuthorizationStrategy);
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        
        FreeStyleProject project;
        try (ACLContext ignored = ACL.as(User.getOrCreateByIdOrFullName(ACL.SYSTEM_USERNAME))) {
            mockAuthorizationStrategy.grant(Job.CREATE).onRoot().to(ACL.SYSTEM_USERNAME);
            project = j.createFreeStyleProject();
        }
        final var uut = new ViolationsJobAction(project);
        final var anonymous = User.getOrCreateByIdOrFullName(ACL.ANONYMOUS_USERNAME);
        // without propper permissions
        try (ACLContext ignored = ACL.as(anonymous)) {
            assertThatThrownBy(() -> uut.getViolationsTrend()).isInstanceOf(AccessDeniedException3.class);
        }
        // with propper permissions
        try (ACLContext ignored = ACL.as(anonymous)) {
            mockAuthorizationStrategy.grant(Job.READ).onItems(project).to(anonymous);
            assertThatCode(() -> uut.getViolationsTrend()).doesNotThrowAnyException();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void getViolationsTrend(JenkinsRule j) throws IOException {
        final var project = j.createFreeStyleProject();
        final var ra1 = mock(ViolationsRunAction.class);
        final var ra2 = mock(ViolationsRunAction.class);
        final var run1 = mock(Run.class);
        final var run2 = mock(Run.class);
        final var v1 = List.of(new Violation("uuid-1", ViolationType.LICENSE, ViolationState.FAIL, "policy-1", null), new Violation("uuid-2", ViolationType.LICENSE, ViolationState.INFO, "policy-2", null), new Violation("uuid-2", ViolationType.OPERATIONAL, ViolationState.INFO, "policy-5", null));
        final var v2 = List.of(new Violation("uuid-1", ViolationType.SECURITY, ViolationState.FAIL, "policy-3", null), new Violation("uuid-2", ViolationType.SECURITY, ViolationState.WARN, "policy-4", null));
        when(run1.getNumber()).thenReturn(1);
        when(run2.getNumber()).thenReturn(2);
        when(ra1.hasViolations()).thenReturn(true);
        when(ra1.getViolations()).thenReturn(v1);
        when(ra1.getRun()).thenReturn(run1);
        when(ra2.hasViolations()).thenReturn(true);
        when(ra2.getViolations()).thenReturn(v2);
        when(ra2.getRun()).thenReturn(run2);
        final FreeStyleBuild b1 = new FreeStyleBuild(project);
        b1.addAction(ra1);
        final FreeStyleBuild b2 = new FreeStyleBuild(project);
        b2.addAction(ra2);
        project._getRuns().put(1, b1);
        project._getRuns().put(2, b2);

        final var uut = new ViolationsJobAction(project);
        final var expected = JSONArray.fromObject(List.of(Map.of("buildNumber", 1, "fail", 1, "info", 2, "warn", 0), Map.of("buildNumber", 2, "fail", 1, "warn", 1, "info", 0)));
        assertThatObject(uut.getViolationsTrend()).isEqualTo(expected);
    }
    
}
