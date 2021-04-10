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
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredRule;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.sf.json.JSONObject;
import org.acegisecurity.AccessDeniedException;
import org.jenkinsci.plugins.DependencyTrack.model.Project;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.kohsuke.stapler.StaplerRequest;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner.StrictStubs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
@RunWith(StrictStubs.class)
public class DescriptorImplTest {

    @Rule
    public JenkinsConfiguredRule r = new JenkinsConfiguredRule();

    @Mock
    private ApiClient client;
    private DescriptorImpl uut;
    private MockAuthorizationStrategy mockAuthorizationStrategy;

    @Before
    public void setup() {
        uut = new DescriptorImpl((url, apiKey, logger, connTimeout, readTimeout) -> client);
        mockAuthorizationStrategy = new MockAuthorizationStrategy();
        r.jenkins.setAuthorizationStrategy(mockAuthorizationStrategy);
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
    }

    @Test
    public void doFillProjectIdItemsTest() throws ApiClientException {
        List<Project> projects = new ArrayList<>();
        projects.add(Project.builder().name("Project 2").uuid("uuid-2").version("1.2.3").build());
        projects.add(Project.builder().name("Project 1").uuid("uuid-1").build());
        doReturn(projects).doThrow(new ApiClientException("test failure"))
                .when(client).getProjects();

        assertThat(uut.doFillProjectIdItems(null, null, null)).usingElementComparatorOnFields("name", "value", "selected").containsExactly(
                new ListBoxModel.Option("-- Select Project --", null, false),
                new ListBoxModel.Option("Project 1", "uuid-1", false),
                new ListBoxModel.Option("Project 2 1.2.3", "uuid-2", false)
        );

        assertThat(uut.doFillProjectIdItems(null, null, null)).usingElementComparatorOnFields("name", "value", "selected").containsExactly(
                new ListBoxModel.Option(Messages.Builder_Error_Projects("test failure"), null, false)
        );
    }

    @Test
    public void doFillDependencyTrackApiKeyItems() throws IOException {
        final String apikey = "api-key";
        final String credentialsid = "credentials-id";
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(), new StringCredentialsImpl(CredentialsScope.GLOBAL, credentialsid, "test", Secret.fromString(apikey)));
        assertThat(uut.doFillDependencyTrackApiKeyItems(null, null)).usingElementComparatorOnFields("name", "value", "selected").containsExactly(
                new ListBoxModel.Option("- none -", "", false),
                new ListBoxModel.Option("test", credentialsid, false)
        );
        assertThat(uut.doFillDependencyTrackApiKeyItems(credentialsid, null)).usingElementComparatorOnFields("name", "value", "selected").containsExactly(
                new ListBoxModel.Option("- none -", "", false),
                new ListBoxModel.Option("test", credentialsid, false)
        );
    }

    @Test
    public void doTestConnectionTest() throws ApiClientException, IOException {
        final String apikey = "api-key";
        final String credentialsid = "credentials-id";
        // custom factory here so we can check that doTestConnection strips trailing slashes from the url
        ApiClientFactory factory = (url, apiKey, logger, connTimeout, readTimeout) -> {
            assertThat(url).isEqualTo("http:///url.tld");
            assertThat(apiKey).isEqualTo(apikey);
            assertThat(logger).isInstanceOf(ConsoleLogger.class);
            return client;
        };
        when(client.testConnection()).thenReturn("Dependency-Track v3.8.0").thenReturn("test").thenThrow(ApiClientException.class);
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(), new StringCredentialsImpl(CredentialsScope.GLOBAL, credentialsid, "test", Secret.fromString(apikey)));
        uut = new DescriptorImpl(factory);

        assertThat(uut.doTestConnection("http:///url.tld", credentialsid, null))
                .hasFieldOrPropertyWithValue("kind", FormValidation.Kind.OK)
                .hasMessage("Connection successful - Dependency-Track v3.8.0")
                .hasNoCause();

        assertThat(uut.doTestConnection("http:///url.tld/", credentialsid, null))
                .hasFieldOrPropertyWithValue("kind", FormValidation.Kind.ERROR)
                .hasMessageStartingWith("Connection failed - test")
                .hasNoCause();

        assertThat(uut.doTestConnection("http:///url.tld/", credentialsid, null))
                .hasFieldOrPropertyWithValue("kind", FormValidation.Kind.ERROR)
                .hasMessageStartingWith("Connection failed")
                .hasMessageContaining(ApiClientException.class.getCanonicalName())
                .hasNoCause();

        assertThat(uut.doTestConnection("url", "", null))
                .hasFieldOrPropertyWithValue("kind", FormValidation.Kind.WARNING)
                .hasMessage("URL must be valid and Api-Key must not be empty")
                .hasNoCause();
    }

    @Test
    public void doTestConnectionPermissionTest() throws IOException {
        final User anonymous = User.getOrCreateByIdOrFullName(ACL.ANONYMOUS_USERNAME);
        // without propper global permissions
        try (ACLContext ignored = ACL.as(anonymous)) {
            assertThatThrownBy(() -> uut.doTestConnection("foo", "", null)).isInstanceOf(AccessDeniedException.class);
        }
        // test for item permissions
        final FreeStyleProject project = r.createFreeStyleProject();
        try (ACLContext ignored = ACL.as(anonymous)) {
            // without item permissions
            assertThatThrownBy(() -> uut.doTestConnection("foo", "", project)).isInstanceOf(AccessDeniedException.class);
            // now grant Item.CONFIGURE
            mockAuthorizationStrategy.grant(Item.CONFIGURE).onItems(project).to(anonymous);
            assertThatCode(() -> uut.doTestConnection("foo", "", project)).doesNotThrowAnyException();
        }
    }

    @Test
    public void doTestConnectionTestWithEmptyArgs() throws ApiClientException, IOException {
        final String apikey = "api-key";
        final String credentialsid = "credentials-id";
        // custom factory here so we can check that doTestConnection strips trailing slashes from the url
        ApiClientFactory factory = (url, apiKey, logger, connTimeout, readTimeout) -> {
            assertThat(url).isEqualTo("http:///url.tld");
            assertThat(apiKey).isEqualTo(apikey);
            assertThat(logger).isInstanceOf(ConsoleLogger.class);
            return client;
        };
        when(client.testConnection()).thenReturn("Dependency-Track v3.8.0").thenReturn("test").thenThrow(ApiClientException.class);
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(), new StringCredentialsImpl(CredentialsScope.GLOBAL, credentialsid, "test", Secret.fromString(apikey)));
        uut = new DescriptorImpl(factory);
        uut.setDependencyTrackApiKey(credentialsid);
        uut.setDependencyTrackUrl("http:///url.tld/");

        assertThat(uut.doTestConnection("", "", null))
                .hasFieldOrPropertyWithValue("kind", FormValidation.Kind.OK)
                .hasMessage("Connection successful - Dependency-Track v3.8.0")
                .hasNoCause();
    }

    @Test
    public void doCheckDependencyTrackUrlTest() {
        assertThat(uut.doCheckDependencyTrackUrl("http://foo.bar/", null)).isEqualTo(FormValidation.ok());
        assertThat(uut.doCheckDependencyTrackUrl("http://foo.bar", null)).isEqualTo(FormValidation.ok());
        assertThat(uut.doCheckDependencyTrackUrl("", null)).isEqualTo(FormValidation.ok());
        assertThat(uut.doCheckDependencyTrackUrl("foo", null))
                .hasFieldOrPropertyWithValue("kind", FormValidation.Kind.ERROR)
                .hasMessage("The specified value is not a valid URL");
    }

    @Test
    public void doCheckDependencyTrackUrlPermissionTest() throws IOException {
        final User anonymous = User.getOrCreateByIdOrFullName(ACL.ANONYMOUS_USERNAME);
        // without propper global permissions
        try (ACLContext ignored = ACL.as(anonymous)) {
            assertThatThrownBy(() -> uut.doCheckDependencyTrackUrl("foo", null)).isInstanceOf(AccessDeniedException.class);
        }
        // test for item permissions
        final FreeStyleProject project = r.createFreeStyleProject();
        try (ACLContext ignored = ACL.as(anonymous)) {
            // without item permissions
            assertThatThrownBy(() -> uut.doCheckDependencyTrackUrl("foo", project)).isInstanceOf(AccessDeniedException.class);
            // now grant Item.CONFIGURE
            mockAuthorizationStrategy.grant(Item.CONFIGURE).onItems(project).to(anonymous);
            assertThatCode(() -> uut.doCheckDependencyTrackUrl("foo", project)).doesNotThrowAnyException();
        }
    }

    @Test
    public void getDependencyTrackUrlTest() {
        uut.setDependencyTrackUrl("http://foo.bar/");
        assertThat(uut.getDependencyTrackUrl()).isEqualTo("http://foo.bar");

        uut.setDependencyTrackUrl("http://foo.bar");
        assertThat(uut.getDependencyTrackUrl()).isEqualTo("http://foo.bar");
    }

    @Test
    public void getDependencyTrackPollingTimeoutTest() {
        assertThat(uut.getDependencyTrackPollingTimeout()).isEqualTo(5);

        uut.setDependencyTrackPollingTimeout(0);
        assertThat(uut.getDependencyTrackPollingTimeout()).isEqualTo(5);

        uut.setDependencyTrackPollingTimeout(Integer.MAX_VALUE);
        assertThat(uut.getDependencyTrackPollingTimeout()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void configureTest() throws Descriptor.FormException {
        StaplerRequest req = mock(StaplerRequest.class);
        JSONObject formData = new JSONObject()
                .element("dependencyTrackUrl", "https://foo.bar/")
                .element("dependencyTrackApiKey", "api-key")
                .element("dependencyTrackAutoCreateProjects", true)
                .element("dependencyTrackPollingTimeout", 7);

        assertThat(uut.configure(req, formData)).isTrue();

        verify(req).bindJSON(eq(uut), eq(formData));
    }
}
