/*
 * Copyright 2020 OWASP.
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

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.security.AccessDeniedException3;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import hudson.util.VersionNumber;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.DependencyTrack.model.Project;
import org.jenkinsci.plugins.DependencyTrack.model.Team;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.StaplerRequest;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jenkinsci.plugins.DependencyTrack.model.Permissions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
@ExtendWith(MockitoExtension.class)
@WithJenkins
class DescriptorImplTest {

    private JenkinsRule r;
    @Mock
    private ApiClient client;
    private DescriptorImpl uut;
    private MockAuthorizationStrategy mockAuthorizationStrategy;
    private final Set<String> requiredPermissions = Stream.of(BOM_UPLOAD, VIEW_PORTFOLIO, VULNERABILITY_ANALYSIS).map(Enum::toString).collect(Collectors.toSet());

    @BeforeEach
    void setup(JenkinsRule r) {
        uut = new DescriptorImpl((url, apiKey, logger, connTimeout, readTimeout) -> client);
        mockAuthorizationStrategy = new MockAuthorizationStrategy();
        mockAuthorizationStrategy.grant(Jenkins.ADMINISTER).onRoot().to(ACL.SYSTEM_USERNAME);
        mockAuthorizationStrategy.grant(Job.CREATE).onRoot().to(ACL.SYSTEM_USERNAME);
        r.jenkins.setAuthorizationStrategy(mockAuthorizationStrategy);
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        this.r = r;
    }

    @Test
    void doFillProjectIdItemsTest() throws ApiClientException {
        List<Project> projects = new ArrayList<>();
        projects.add(Project.builder().name("Project 2").uuid("uuid-2").version("1.2.3").build());
        projects.add(Project.builder().name("Project 1").uuid("uuid-1").build());
        doReturn(projects).doThrow(new ApiClientException("test failure"))
                .when(client).getProjects();

        try (ACLContext ignored = ACL.as(User.getOrCreateByIdOrFullName(ACL.SYSTEM_USERNAME))) {
            assertThat(uut.doFillProjectIdItems(null, null, null)).usingRecursiveFieldByFieldElementComparatorOnFields("name", "value", "selected").containsExactly(
                    new ListBoxModel.Option(Messages.Publisher_ProjectList_Placeholder(), "", false),
                    new ListBoxModel.Option("Project 1", "uuid-1", false),
                    new ListBoxModel.Option("Project 2 1.2.3", "uuid-2", false)
            );

            assertThat(uut.doFillProjectIdItems(null, null, null)).usingRecursiveFieldByFieldElementComparatorOnFields("name", "value", "selected").containsExactly(
                    new ListBoxModel.Option(Messages.Builder_Error_Projects("test failure"), "", false)
            );
        }
    }

    @Test
    void doFillDependencyTrackApiKeyItems() throws IOException {
        final String apikey = "api-key";
        final String credentialsid = "credentials-id";
        try (ACLContext ignored = ACL.as(User.getOrCreateByIdOrFullName(ACL.SYSTEM_USERNAME))) {
            CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(), new StringCredentialsImpl(CredentialsScope.GLOBAL, credentialsid, "test", Secret.fromString(apikey)));
            assertThat(uut.doFillDependencyTrackApiKeyItems(null, null)).usingRecursiveFieldByFieldElementComparatorOnFields("name", "value", "selected").containsExactly(
                    new ListBoxModel.Option("- none -", "", false),
                    new ListBoxModel.Option("test", credentialsid, false)
            );
            assertThat(uut.doFillDependencyTrackApiKeyItems(credentialsid, null)).usingRecursiveFieldByFieldElementComparatorOnFields("name", "value", "selected").containsExactly(
                    new ListBoxModel.Option("- none -", "", false),
                    new ListBoxModel.Option("test", credentialsid, false)
            );
        }
    }

    @Test
    void doTestConnectionTest() throws IOException {
        final String apikey = "api-key";
        final String credentialsid = "credentials-id";
        // custom factory here so we can check that doTestConnection strips trailing slashes from the url
        ApiClientFactory factory = (url, apiKey, logger, connTimeout, readTimeout) -> {
            assertThat(url).isEqualTo("http:///url.tld");
            assertThat(apiKey).isEqualTo(apikey);
            assertThat(logger).isInstanceOf(ConsoleLogger.class);
            return client;
        };
        when(client.testConnection()).thenReturn("Dependency-Track v3.8.0", "test").thenThrow(ApiClientException.class);
        when(client.getVersion()).thenReturn(new VersionNumber("3.8.0"));
        try (ACLContext ignored = ACL.as(User.getOrCreateByIdOrFullName(ACL.SYSTEM_USERNAME))) {
            CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(), new StringCredentialsImpl(CredentialsScope.GLOBAL, credentialsid, "test", Secret.fromString(apikey)));
            uut = new DescriptorImpl(factory);

            assertThat(uut.doTestConnectionGlobal("http:///url.tld", credentialsid, false, null))
                    .hasFieldOrPropertyWithValue("kind", FormValidation.Kind.ERROR)
                    .hasMessage(Messages.Publisher_ConnectionTest_VersionWarning("3.8.0", "4.7.0"))
                    .hasNoCause();

            assertThat(uut.doTestConnectionGlobal("http:///url.tld/", credentialsid, false, null))
                    .hasFieldOrPropertyWithValue("kind", FormValidation.Kind.ERROR)
                    .hasMessageStartingWith(Messages.Publisher_ConnectionTest_Error("test"))
                    .hasNoCause();

            assertThat(uut.doTestConnectionGlobal("http:///url.tld/", credentialsid, false, null))
                    .hasFieldOrPropertyWithValue("kind", FormValidation.Kind.ERROR)
                    .hasMessageStartingWith(Messages.Publisher_ConnectionTest_Error(null))
                    .hasMessageContaining(ApiClientException.class.getCanonicalName())
                    .hasNoCause();

            assertThat(uut.doTestConnectionGlobal("url", "", false, null))
                    .hasFieldOrPropertyWithValue("kind", FormValidation.Kind.ERROR)
                    .hasMessage(Messages.Publisher_ConnectionTest_InputError())
                    .hasNoCause();
        }
    }

    @Test
    void doTestConnectionPermissionTest() throws IOException {
        final User anonymous = User.getOrCreateByIdOrFullName(ACL.ANONYMOUS_USERNAME);
        // without propper global permissions
        try (ACLContext ignored = ACL.as(anonymous)) {
            assertThatThrownBy(() -> uut.doTestConnectionGlobal("foo", "", false, null)).isInstanceOf(AccessDeniedException3.class);
        }
        // test for item permissions
        FreeStyleProject project;
        try (ACLContext ignored = ACL.as(User.getOrCreateByIdOrFullName(ACL.SYSTEM_USERNAME))) {
            project = r.createFreeStyleProject();
        }
        try (ACLContext ignored = ACL.as(anonymous)) {
            // without item permissions
            assertThatThrownBy(() -> uut.doTestConnectionGlobal("foo", "", false, project)).isInstanceOf(AccessDeniedException3.class);
            // now grant Item.CONFIGURE
            mockAuthorizationStrategy.grant(Item.CONFIGURE).onItems(project).to(anonymous);
            assertThatCode(() -> uut.doTestConnectionGlobal("foo", "", false, project)).doesNotThrowAnyException();
        }
    }

    @Test
    void doTestConnectionTestWithEmptyArgs() throws ApiClientException, IOException {
        final String apikey = "api-key";
        final String credentialsid = "credentials-id";
        // custom factory here so we can check that doTestConnection strips trailing slashes from the url
        ApiClientFactory factory = (url, apiKey, logger, connTimeout, readTimeout) -> {
            assertThat(url).isEqualTo("http:///url.tld");
            assertThat(apiKey).isEqualTo(apikey);
            assertThat(logger).isInstanceOf(ConsoleLogger.class);
            return client;
        };
        when(client.testConnection()).thenReturn("Dependency-Track v4.7.0");
        when(client.getVersion()).thenReturn(new VersionNumber("4.7.0"));
        when(client.getTeamPermissions()).thenReturn(Team.builder().name("my-team").permissions(requiredPermissions).build());
        try (ACLContext ignored = ACL.as(User.getOrCreateByIdOrFullName(ACL.SYSTEM_USERNAME))) {
            CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(), new StringCredentialsImpl(CredentialsScope.GLOBAL, credentialsid, "test", Secret.fromString(apikey)));
            uut = new DescriptorImpl(factory);
            uut.setDependencyTrackApiKey(credentialsid);
            uut.setDependencyTrackUrl("http:///url.tld/");

            assertThat(uut.doTestConnectionJob("", "", false, false, false, null))
                    .hasFieldOrPropertyWithValue("kind", FormValidation.Kind.OK)
                    .hasNoCause();
        }
    }

    @Test
    void doTestConnectionTestWithTeamPermissions() throws IOException {
        final String apikey = "api-key";
        final String credentialsid = "credentials-id";
        final Team team = Team.builder()
                .name("test-team")
                .permissions(Stream.of(BOM_UPLOAD, VIEW_PORTFOLIO, VULNERABILITY_ANALYSIS, PROJECT_CREATION_UPLOAD, PORTFOLIO_MANAGEMENT).map(Enum::toString).collect(Collectors.toSet()))
                .build();
        // custom factory here so we can check that doTestConnection strips trailing slashes from the url
        ApiClientFactory factory = (url, apiKey, logger, connTimeout, readTimeout) -> {
            assertThat(url).isEqualTo("http:///url.tld");
            assertThat(apiKey).isEqualTo(apikey);
            assertThat(logger).isInstanceOf(ConsoleLogger.class);
            return client;
        };
        final var poweredBy = "Dependency-Track v4.7.0";
        when(client.testConnection()).thenReturn(poweredBy);
        when(client.getVersion()).thenReturn(new VersionNumber("4.7.0"));
        when(client.getTeamPermissions()).thenReturn(team);
        try (ACLContext ignored = ACL.as(User.getOrCreateByIdOrFullName(ACL.SYSTEM_USERNAME))) {
            CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(), new StringCredentialsImpl(CredentialsScope.GLOBAL, credentialsid, "test", Secret.fromString(apikey)));
            uut = new DescriptorImpl(factory);

            assertThat(uut.doTestConnectionJob("http:///url.tld", credentialsid, true, false, false, null))
                    .hasFieldOrPropertyWithValue("kind", FormValidation.Kind.OK)
                    .hasNoCause()
                    .extracting(FormValidation::renderHtml).asString()
                    .contains(Messages.Publisher_ConnectionTest_Success(poweredBy))
                    .contains(Messages.Publisher_PermissionTest_Team(team.getName()))
                    .contains(Messages.Publisher_PermissionTest_Okay(BOM_UPLOAD))
                    .contains(Messages.Publisher_PermissionTest_Okay(VIEW_PORTFOLIO))
                    .contains(Messages.Publisher_PermissionTest_Okay(VULNERABILITY_ANALYSIS))
                    .contains(Messages.Publisher_PermissionTest_Okay(PROJECT_CREATION_UPLOAD))
                    .contains(Messages.Publisher_PermissionTest_Optional(PORTFOLIO_MANAGEMENT))
                    .contains(team.getPermissions().toArray(String[]::new));
        }
    }

    @Test
    void doTestConnectionTestWithMissingRequiredPermission() throws IOException {
        final String apikey = "api-key";
        final String credentialsid = "credentials-id";
        final Team team = Team.builder()
                .name("test-team")
                // missing VIEW_VULNERABILITY and PORTFOLIO_MANAGEMENT
                .permissions(Stream.of(BOM_UPLOAD, VIEW_PORTFOLIO, VULNERABILITY_ANALYSIS, PROJECT_CREATION_UPLOAD).map(Enum::toString).collect(Collectors.toSet()))
                .build();
        // custom factory here so we can check that doTestConnection strips trailing slashes from the url
        ApiClientFactory factory = (url, apiKey, logger, connTimeout, readTimeout) -> {
            assertThat(url).isEqualTo("http:///url.tld");
            assertThat(apiKey).isEqualTo(apikey);
            assertThat(logger).isInstanceOf(ConsoleLogger.class);
            return client;
        };
        final var poweredBy = "Dependency-Track v4.7.0";
        when(client.testConnection()).thenReturn(poweredBy);
        when(client.getVersion()).thenReturn(new VersionNumber("4.7.0"));
        when(client.getTeamPermissions()).thenReturn(team);
        try (ACLContext ignored = ACL.as(User.getOrCreateByIdOrFullName(ACL.SYSTEM_USERNAME))) {
            CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(), new StringCredentialsImpl(CredentialsScope.GLOBAL, credentialsid, "test", Secret.fromString(apikey)));
            uut = new DescriptorImpl(factory);

            assertThat(uut.doTestConnectionJob("http:///url.tld", credentialsid, true, true, true, null))
                    .hasFieldOrPropertyWithValue("kind", FormValidation.Kind.ERROR)
                    .hasNoCause()
                    .extracting(FormValidation::renderHtml).asString()
                    .contains(Messages.Publisher_ConnectionTest_Error(poweredBy))
                    .contains(Messages.Publisher_PermissionTest_Team(team.getName()))
                    .contains(Messages.Publisher_PermissionTest_Okay(BOM_UPLOAD))
                    .contains(Messages.Publisher_PermissionTest_Okay(VIEW_PORTFOLIO))
                    .contains(Messages.Publisher_PermissionTest_Okay(VULNERABILITY_ANALYSIS))
                    .contains(Messages.Publisher_PermissionTest_Okay(PROJECT_CREATION_UPLOAD))
                    .contains(Messages.Publisher_PermissionTest_Missing(PORTFOLIO_MANAGEMENT))
                    .contains(Messages.Publisher_PermissionTest_Missing(VIEW_VULNERABILITY))
                    .contains(team.getPermissions().toArray(String[]::new));
        }
    }

    @Test
    void doTestConnectionTestWithUnneededPermission() throws IOException {
        final String apikey = "api-key";
        final String credentialsid = "credentials-id";
        final Team team = Team.builder()
                .name("test-team")
                .permissions(Stream.of("BOM_UPLOAD", "VIEW_PORTFOLIO", "VULNERABILITY_ANALYSIS", "PROJECT_CREATION_UPLOAD", "DUMMY_PERM").collect(Collectors.toSet()))
                .build();
        // custom factory here so we can check that doTestConnection strips trailing slashes from the url
        ApiClientFactory factory = (url, apiKey, logger, connTimeout, readTimeout) -> {
            assertThat(url).isEqualTo("http:///url.tld");
            assertThat(apiKey).isEqualTo(apikey);
            assertThat(logger).isInstanceOf(ConsoleLogger.class);
            return client;
        };
        final var poweredBy = "Dependency-Track v4.7.0";
        when(client.testConnection()).thenReturn(poweredBy);
        when(client.getVersion()).thenReturn(new VersionNumber("4.7.0"));
        when(client.getTeamPermissions()).thenReturn(team);
        try (ACLContext ignored = ACL.as(User.getOrCreateByIdOrFullName(ACL.SYSTEM_USERNAME))) {
            CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(), new StringCredentialsImpl(CredentialsScope.GLOBAL, credentialsid, "test", Secret.fromString(apikey)));
            uut = new DescriptorImpl(factory);

            assertThat(uut.doTestConnectionJob("http:///url.tld", credentialsid, true, false, false, null))
                    .hasFieldOrPropertyWithValue("kind", FormValidation.Kind.WARNING)
                    .hasNoCause()
                    .extracting(FormValidation::renderHtml).asString()
                    .contains(Messages.Publisher_ConnectionTest_Warning(poweredBy))
                    .contains(Messages.Publisher_PermissionTest_Team(team.getName()))
                    .contains(Messages.Publisher_PermissionTest_Okay(BOM_UPLOAD))
                    .contains(Messages.Publisher_PermissionTest_Okay(VIEW_PORTFOLIO))
                    .contains(Messages.Publisher_PermissionTest_Okay(VULNERABILITY_ANALYSIS))
                    .contains(Messages.Publisher_PermissionTest_Okay(PROJECT_CREATION_UPLOAD))
                    .contains(Messages.Publisher_PermissionTest_Warning("DUMMY_PERM"))
                    .contains(team.getPermissions().toArray(String[]::new));
        }
    }

    @Test
    void doCheckDependencyTrackUrlTest() {
        try (ACLContext ignored = ACL.as(User.getOrCreateByIdOrFullName(ACL.SYSTEM_USERNAME))) {
            assertThat(uut.doCheckDependencyTrackUrl("http://foo.bar/", null)).isEqualTo(FormValidation.ok());
            assertThat(uut.doCheckDependencyTrackUrl("http://foo.bar", null)).isEqualTo(FormValidation.ok());
            assertThat(uut.doCheckDependencyTrackUrl("", null)).isEqualTo(FormValidation.ok());
            assertThat(uut.doCheckDependencyTrackUrl("foo", null))
                    .hasFieldOrPropertyWithValue("kind", FormValidation.Kind.ERROR)
                    .hasMessage("The specified value is not a valid URL");
        }
    }

    @Test
    void doCheckDependencyTrackUrlPermissionTest() throws IOException {
        final User anonymous = User.getOrCreateByIdOrFullName(ACL.ANONYMOUS_USERNAME);
        // without propper global permissions
        try (ACLContext ignored = ACL.as(anonymous)) {
            assertThatThrownBy(() -> uut.doCheckDependencyTrackUrl("foo", null)).isInstanceOf(AccessDeniedException3.class);
        }
        // test for item permissions
        FreeStyleProject project;
        try (ACLContext ignored = ACL.as(User.getOrCreateByIdOrFullName(ACL.SYSTEM_USERNAME))) {
            project = r.createFreeStyleProject();
        }
        try (ACLContext ignored = ACL.as(anonymous)) {
            // without item permissions
            assertThatThrownBy(() -> uut.doCheckDependencyTrackUrl("foo", project)).isInstanceOf(AccessDeniedException3.class);
            // now grant Item.CONFIGURE
            mockAuthorizationStrategy.grant(Item.CONFIGURE).onItems(project).to(anonymous);
            assertThatCode(() -> uut.doCheckDependencyTrackUrl("foo", project)).doesNotThrowAnyException();
        }
    }

    @Test
    void getDependencyTrackUrlTest() {
        uut.setDependencyTrackUrl("http://foo.bar/");
        assertThat(uut.getDependencyTrackUrl()).isEqualTo("http://foo.bar");

        uut.setDependencyTrackUrl("http://foo.bar");
        assertThat(uut.getDependencyTrackUrl()).isEqualTo("http://foo.bar");
    }

    @Test
    void getDependencyTrackPollingTimeoutTest() {
        assertThat(uut.getDependencyTrackPollingTimeout()).isEqualTo(5);

        uut.setDependencyTrackPollingTimeout(0);
        assertThat(uut.getDependencyTrackPollingTimeout()).isEqualTo(5);

        uut.setDependencyTrackPollingTimeout(Integer.MAX_VALUE);
        assertThat(uut.getDependencyTrackPollingTimeout()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void configureTest() throws Descriptor.FormException {
        StaplerRequest req = mock(StaplerRequest.class);
        JSONObject formData = new JSONObject()
                .element("dependencyTrackUrl", "https://foo.bar/")
                .element("dependencyTrackApiKey", "api-key")
                .element("dependencyTrackAutoCreateProjects", true)
                .element("dependencyTrackPollingTimeout", 7);

        assertThat(uut.configure(req, formData)).isTrue();

        verify(req).bindJSON(uut, formData);
    }
}
