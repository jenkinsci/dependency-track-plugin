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

import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredRule;
import java.util.ArrayList;
import java.util.List;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.DependencyTrack.model.Project;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.kohsuke.stapler.StaplerRequest;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
public class DescriptorImplTest {

    @Rule
    public JenkinsConfiguredRule r = new JenkinsConfiguredRule();
    
    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);
    
    @Mock
    private ApiClient client;
    private DescriptorImpl uut;
    
    @Before
    public void setup() {        
        uut = new DescriptorImpl((url, apiKey, logger) -> client);
    }

    @Test
    public void doFillProjectIdItemsTest() throws ApiClientException {
        List<Project> projects = new ArrayList<>();
        projects.add(Project.builder().name("Project 1").uuid("uuid-1").build());
        projects.add(Project.builder().name("Project 2").uuid("uuid-2").version("1.2.3").build());
        doReturn(projects).doThrow(new ApiClientException("test failure"))
                .when(client).getProjects();
        
        assertThat(uut.doFillProjectIdItems()).usingElementComparatorOnFields("name", "value", "selected").containsExactly(
                new ListBoxModel.Option("-- Select Project --", null, false),
                new ListBoxModel.Option("Project 1", "uuid-1", false),
                new ListBoxModel.Option("Project 2 1.2.3", "uuid-2", false)
        );
        
        assertThat(uut.doFillProjectIdItems()).usingElementComparatorOnFields("name", "value", "selected").containsExactly(
                new ListBoxModel.Option(Messages.Builder_Error_Projects() + ": test failure", null, false)
        );
    }

    @Test
    public void doTestConnectionTest() throws ApiClientException {
        when(client.testConnection()).thenReturn("test").thenThrow(ApiClientException.class);
        
        assertThat(uut.doTestConnection("url", "key"))
                .hasFieldOrPropertyWithValue("kind", FormValidation.Kind.OK)
                .hasMessage("Connection successful - test")
                .hasNoCause();
        
        assertThat(uut.doTestConnection("url", "key"))
                .hasFieldOrPropertyWithValue("kind", FormValidation.Kind.ERROR)
                .hasMessageStartingWith("Connection failed")
                .hasMessageContaining(ApiClientException.class.getCanonicalName())
                .hasNoCause();
    }
    
    @Test
    public void doCheckDependencyTrackUrlTest() {
        assertThat(uut.doCheckDependencyTrackUrl("http://foo.bar/")).isEqualTo(FormValidation.ok());
        assertThat(uut.doCheckDependencyTrackUrl("http://foo.bar")).isEqualTo(FormValidation.ok());
        assertThat(uut.doCheckDependencyTrackUrl("")).isEqualTo(FormValidation.ok());
        assertThat(uut.doCheckDependencyTrackUrl("foo"))
                .hasFieldOrPropertyWithValue("kind", FormValidation.Kind.ERROR)
                .hasMessage("The specified value is not a valid URL");
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
        
        uut.configure(req, formData);
        
        verify(req).bindJSON(eq(uut), eq(formData));
        assertThat(uut.getDependencyTrackUrl()).isEqualTo("https://foo.bar");
        assertThat(uut.getDependencyTrackApiKey()).isEqualTo("api-key");
        assertThat(uut.isDependencyTrackAutoCreateProjects()).isTrue();
        assertThat(uut.getDependencyTrackPollingTimeout()).isEqualTo(7);
    }
}
