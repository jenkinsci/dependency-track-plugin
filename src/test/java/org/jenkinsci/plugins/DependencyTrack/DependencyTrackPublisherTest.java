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
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.Secret;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import org.jenkinsci.plugins.DependencyTrack.model.Project;
import org.jenkinsci.plugins.DependencyTrack.model.SeverityDistribution;
import org.jenkinsci.plugins.DependencyTrack.model.UploadResult;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner.StrictStubs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
@RunWith(StrictStubs.class)
public class DependencyTrackPublisherTest {

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Mock
    private Run build;

    @Mock
    private TaskListener listener;

    @Mock
    private Launcher launcher;

    private final EnvVars env = new EnvVars("my.var", "my.value");

    @Mock
    private Job job;

    @Mock
    private ApiClient client;

    private final ApiClientFactory clientFactory = (url, apiKey, logger, connTimeout, readTimeout) -> client;
    private final String apikeyId = "api-key-id";
    private final String apikey = "api-key";

    @Before
    public void setup() throws ApiClientException, IOException {
        when(listener.getLogger()).thenReturn(System.err);

        DescriptorImpl descriptor = r.jenkins.getDescriptorByType(DescriptorImpl.class);
        descriptor.setDependencyTrackPollingInterval(1);
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(), new StringCredentialsImpl(CredentialsScope.GLOBAL, apikeyId, "DependencyTrackPublisherTest", Secret.fromString(apikey)));

        // needed for credential tracking
        when(job.getParent()).thenReturn(r.jenkins);
        when(job.getName()).thenReturn("u-drive-me-crazy");
        when(build.getParent()).thenReturn(job);
        when(build.getNumber()).thenReturn(1);
    }

    @Test
    public void testPerformPrechecks() throws IOException {
        when(listener.getLogger()).thenReturn(System.err);
        FilePath workDir = new FilePath(tmpDir.getRoot());

        // artifact missing
        final DependencyTrackPublisher uut1 = new DependencyTrackPublisher("", false, clientFactory);
        assertThatCode(() -> uut1.perform(build, workDir, env, launcher, listener)).isInstanceOf(AbortException.class).hasMessage(Messages.Builder_Artifact_Unspecified());

        File artifact = tmpDir.newFile();
        // uuid and name and version missing
        final DependencyTrackPublisher uut2 = new DependencyTrackPublisher(artifact.getName(), false, clientFactory);
        assertThatCode(() -> uut2.perform(build, workDir, env, launcher, listener)).isInstanceOf(AbortException.class).hasMessage(Messages.Builder_Result_InvalidArguments());

        // version missing
        final DependencyTrackPublisher uut3 = new DependencyTrackPublisher(artifact.getName(), false, clientFactory);
        uut3.setProjectName("name");
        assertThatCode(() -> uut3.perform(build, workDir, env, launcher, listener)).isInstanceOf(AbortException.class).hasMessage(Messages.Builder_Result_InvalidArguments());

        // name missing
        final DependencyTrackPublisher uut4 = new DependencyTrackPublisher(artifact.getName(), false, clientFactory);
        uut4.setProjectVersion("version");
        assertThatCode(() -> uut4.perform(build, workDir, env, launcher, listener)).isInstanceOf(AbortException.class).hasMessage(Messages.Builder_Result_InvalidArguments());

        // file not within workdir
        final DependencyTrackPublisher uut5 = new DependencyTrackPublisher("foo", false, clientFactory);
        uut5.setProjectId("uuid-1");
        assertThatCode(() -> uut5.perform(build, workDir, env, launcher, listener)).isInstanceOf(AbortException.class).hasMessage(Messages.Builder_Artifact_NonExist("foo"));

        // uuid missing
        final DependencyTrackPublisher uut6 = new DependencyTrackPublisher(artifact.getName(), false, clientFactory);
        uut6.setProjectName("name");
        uut6.setProjectVersion("version");
        assertThatCode(() -> uut6.perform(build, workDir, env, launcher, listener)).isInstanceOf(AbortException.class).hasMessage(Messages.Builder_Result_ProjectIdMissing());
    }

    @Test
    public void doNotThrowNPEinGetEffectiveApiKey() throws IOException {
        File tmp = tmpDir.newFile();
        FilePath workDir = new FilePath(tmpDir.getRoot());
        final DependencyTrackPublisher uut = new DependencyTrackPublisher(tmp.getName(), false, clientFactory);
        uut.setProjectId("uuid-1");

        when(client.upload(eq("uuid-1"), isNull(), isNull(), any(FilePath.class), eq(false))).thenThrow(new ApiClientException(Messages.ApiClient_Error_Connection("", "")));

        assertThatCode(() -> uut.perform(build, workDir, env, launcher, listener)).isInstanceOf(ApiClientException.class).hasMessage(Messages.ApiClient_Error_Connection("", ""));
    }

    @Test
    public void testPerformAsync() throws IOException {
        File tmp = tmpDir.newFile();
        FilePath workDir = new FilePath(tmpDir.getRoot());
        final DependencyTrackPublisher uut = new DependencyTrackPublisher(tmp.getName(), false, clientFactory);
        uut.setProjectId("uuid-1");
        uut.setDependencyTrackApiKey(apikeyId);
        uut.setProjectProperties(new ProjectProperties());

        when(client.upload(eq("uuid-1"), isNull(), isNull(), any(FilePath.class), eq(false)))
                .thenReturn(new UploadResult(true))
                .thenReturn(new UploadResult(false));

        assertThatCode(() -> uut.perform(build, workDir, env, launcher, listener)).doesNotThrowAnyException();
        verify(client, never()).getFindings(anyString());
        verify(client, never()).lookupProject(anyString(), anyString());
        verify(client).updateProjectProperties(eq("uuid-1"), any(ProjectProperties.class));

        assertThatCode(() -> uut.perform(build, workDir, env, launcher, listener)).isInstanceOf(AbortException.class).hasMessage(Messages.Builder_Upload_Failed());
    }

    @Test
    public void testPerformAsyncWithoutProjectId() throws IOException {
        File tmp = tmpDir.newFile();
        FilePath workDir = new FilePath(tmpDir.getRoot());
        final DependencyTrackPublisher uut = new DependencyTrackPublisher(tmp.getName(), false, clientFactory);
        uut.setProjectName("name-1");
        uut.setProjectVersion("${my.var}");
        uut.setDependencyTrackApiKey(apikeyId);
        uut.setAutoCreateProjects(Boolean.TRUE);

        when(client.upload(isNull(), eq("name-1"), eq("my.value"), any(FilePath.class), eq(true))).thenReturn(new UploadResult(true, "token-1"));

        assertThatCode(() -> uut.perform(build, workDir, env, launcher, listener)).doesNotThrowAnyException();
        verify(client, never()).lookupProject(anyString(), anyString());
        verify(client, never()).getFindings(anyString());
    }

    @Test
    public void testPerformSync() throws IOException {
        File tmp = tmpDir.newFile();
        FilePath workDir = new FilePath(tmpDir.getRoot());
        DependencyTrackPublisher uut = new DependencyTrackPublisher(tmp.getName(), true, clientFactory);
        uut.setProjectId("uuid-1");
        uut.setDependencyTrackApiKey(apikeyId);

        when(client.upload(eq("uuid-1"), isNull(), isNull(), any(FilePath.class), eq(false))).thenReturn(new UploadResult(true, "token-1"));
        when(client.isTokenBeingProcessed("token-1")).thenReturn(Boolean.TRUE).thenReturn(Boolean.FALSE);
        when(client.getFindings("uuid-1")).thenReturn(Collections.emptyList());
        
        Run buildWithResultAction = mock(Run.class);
        when(buildWithResultAction.getResult()).thenReturn(Result.SUCCESS);
        when(buildWithResultAction.getAction(ResultAction.class)).thenReturn(new ResultAction(Collections.emptyList(), new SeverityDistribution(42)));
        Run buildWithNoResultAction = mock(Run.class);
        when(buildWithNoResultAction.getResult()).thenReturn(Result.SUCCESS);
        when(buildWithNoResultAction.getPreviousSuccessfulBuild()).thenReturn(buildWithResultAction);
        Run abortedBuild = mock(Run.class);
        when(abortedBuild.getResult()).thenReturn(Result.NOT_BUILT);
        when(abortedBuild.getPreviousSuccessfulBuild()).thenReturn(buildWithNoResultAction);
        when(build.getPreviousSuccessfulBuild()).thenReturn(abortedBuild);

        assertThatCode(() -> uut.perform(build, workDir, env, launcher, listener)).doesNotThrowAnyException();
        verify(client, times(2)).isTokenBeingProcessed("token-1");
        verify(client).getFindings("uuid-1");
        verify(buildWithResultAction, times(2)).getAction(ResultAction.class);
    }

    @Test
    public void testPerformSyncWithoutProjectId() throws IOException {
        File tmp = tmpDir.newFile();
        FilePath workDir = new FilePath(tmpDir.getRoot());
        DependencyTrackPublisher uut = new DependencyTrackPublisher(tmp.getName(), true, clientFactory);
        uut.setProjectName("name-1");
        uut.setProjectVersion("version-1");
        uut.setDependencyTrackApiKey(apikeyId);
        uut.setAutoCreateProjects(Boolean.TRUE);
        uut.setProjectProperties(new ProjectProperties());

        when(client.upload(isNull(), eq("name-1"), eq("version-1"), any(FilePath.class), eq(true))).thenReturn(new UploadResult(true, "token-1"));
        when(client.isTokenBeingProcessed("token-1")).thenReturn(Boolean.TRUE).thenReturn(Boolean.FALSE);
        when(client.getFindings("uuid-1")).thenReturn(Collections.emptyList());
        when(client.lookupProject("name-1", "version-1")).thenReturn(Project.builder().uuid("uuid-1").build());

        assertThatCode(() -> uut.perform(build, workDir, env, launcher, listener)).doesNotThrowAnyException();
        assertThat(uut.getProjectId()).isNullOrEmpty();
        verify(client, times(2)).isTokenBeingProcessed("token-1");
        verify(client).getFindings("uuid-1");
        verify(client).updateProjectProperties(eq("uuid-1"), any(ProjectProperties.class));
        verify(client).lookupProject("name-1", "version-1");
    }

    @Test
    public void testUseOfOverridenProperties() throws IOException {
        File tmp = tmpDir.newFile();
        FilePath workDir = new FilePath(tmpDir.getRoot());
        ApiClientFactory factory = (url, apiKey, logger, connTimeout, readTimeout) -> {
            assertThat(url).isEqualTo("http://test.tld");
            assertThat(apiKey).isEqualTo(apikey);
            assertThat(logger).isInstanceOf(ConsoleLogger.class);
            return client;
        };
        final DependencyTrackPublisher uut = new DependencyTrackPublisher(tmp.getName(), false, factory);
        uut.setProjectId("uuid-1");
        uut.setAutoCreateProjects(Boolean.TRUE);
        uut.setDependencyTrackUrl("http://test.tld");
        uut.setDependencyTrackApiKey(apikeyId);

        when(client.upload(eq("uuid-1"), isNull(), isNull(), any(FilePath.class), eq(true)))
                .thenReturn(new UploadResult(false));

        assertThatCode(() -> uut.perform(build, workDir, env, launcher, listener)).isInstanceOf(AbortException.class).hasMessage(Messages.Builder_Upload_Failed());
    }

    @Test
    public void serializationTest() throws IOException, ClassNotFoundException {
        File tmp = tmpDir.newFile();
        DependencyTrackPublisher uut = new DependencyTrackPublisher(tmp.getName(), true, clientFactory);
        uut.setAutoCreateProjects(Boolean.TRUE);
        uut.setDependencyTrackUrl("foo");
        uut.setDependencyTrackFrontendUrl("foo-ui");
        uut.setDependencyTrackApiKey("bar");
        uut.setOverrideGlobals(false);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(uut);
        }

        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            assertThat(ois.readObject()).isInstanceOfSatisfying(DependencyTrackPublisher.class, actual -> {
                assertThat(actual.getDependencyTrackUrl()).isNull();
                assertThat(actual.getDependencyTrackFrontendUrl()).isNull();
                assertThat(actual.getDependencyTrackApiKey()).isNull();
                assertThat(actual.getAutoCreateProjects()).isNull();
                assertThat(actual.isOverrideGlobals()).isFalse();
            });
        }
    }

    @Test
    public void deserializationTest() throws IOException, ClassNotFoundException {
        File tmp = tmpDir.newFile();
        DependencyTrackPublisher uut = new DependencyTrackPublisher(tmp.getName(), true, clientFactory);
        uut.setAutoCreateProjects(Boolean.TRUE);
        uut.setDependencyTrackUrl("foo");
        uut.setDependencyTrackFrontendUrl("foo-ui");
        uut.setDependencyTrackApiKey("bar");
        uut.setOverrideGlobals(true);
        uut.setProjectProperties(new ProjectProperties());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(uut);
        }

        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            assertThat(ois.readObject()).isInstanceOfSatisfying(DependencyTrackPublisher.class, actual -> {
                assertThat(actual.getDependencyTrackUrl()).isEqualTo("foo");
                assertThat(actual.getDependencyTrackFrontendUrl()).isEqualTo("foo-ui");
                assertThat(actual.getDependencyTrackApiKey()).isEqualTo("bar");
                assertThat(actual.getAutoCreateProjects()).isTrue();
                assertThat(actual.isOverrideGlobals()).isTrue();
                assertThat(actual.getProjectProperties()).isNotNull();
            });
        }
    }

}
