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

import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredRule;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import org.jenkinsci.plugins.DependencyTrack.model.UploadResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
public class DependencyTrackPublisherTest {
    
    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);
    
    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Rule
    public JenkinsConfiguredRule r = new JenkinsConfiguredRule();
    
    @Mock
    private Run build;
    
    @Mock
    private TaskListener listener;
    
    @Mock
    private Launcher launcher;
    
    @Mock
    private ApiClient client;
    
    private final ApiClientFactory clientFactory = (url, apiKey, logger) -> client;
    
    @Before
    public void setup() {        
        when(listener.getLogger()).thenReturn(System.err);
    }

    @Test
    public void testPerformPrechecks() throws IOException {
        FilePath workDir = new FilePath(tmpDir.getRoot());
        final DependencyTrackPublisher uut1 = new DependencyTrackPublisher("", false, clientFactory);
        assertThatCode(() -> uut1.perform(build, workDir, launcher, listener)).isInstanceOf(AbortException.class).hasMessage("Artifact not specified");
        
        File artifact = tmpDir.newFile();
        // uuid and name and version missing
        final DependencyTrackPublisher uut2 = new DependencyTrackPublisher(artifact.getName(), false, clientFactory);
        assertThatCode(() -> uut2.perform(build, workDir, launcher, listener)).isInstanceOf(AbortException.class).hasMessage("Invalid arguments");
        
        // version missing
        final DependencyTrackPublisher uut3 = new DependencyTrackPublisher(artifact.getName(), false, clientFactory);
        uut2.setProjectName("name");
        assertThatCode(() -> uut3.perform(build, workDir, launcher, listener)).isInstanceOf(AbortException.class).hasMessage("Invalid arguments");
        
        // name missing
        final DependencyTrackPublisher uut4 = new DependencyTrackPublisher(artifact.getName(), false, clientFactory);
        uut2.setProjectVersion("version");
        assertThatCode(() -> uut4.perform(build, workDir, launcher, listener)).isInstanceOf(AbortException.class).hasMessage("Invalid arguments");
        
        // file not within workdir
        final DependencyTrackPublisher uut5 = new DependencyTrackPublisher("foo", false, clientFactory);
        uut2.setProjectId("uuid-1");
        assertThatCode(() -> uut5.perform(build, workDir, launcher, listener)).isInstanceOf(AbortException.class).hasMessage("Invalid arguments");
    }

    @Test
    public void testPerformAsync() throws IOException {
        File tmp = tmpDir.newFile();
        FilePath workDir = new FilePath(tmpDir.getRoot());
        final DependencyTrackPublisher uut = new DependencyTrackPublisher(tmp.getName(), false, clientFactory);
        uut.setProjectId("uuid-1");
        
        when(client.upload(eq("uuid-1"), isNull(), isNull(), any(FilePath.class), eq(false)))
                .thenReturn(new UploadResult(true))
                .thenReturn(new UploadResult(false));
        
        assertThatCode(() -> uut.perform(build, workDir, launcher, listener)).doesNotThrowAnyException();
        verify(client, never()).getFindings(anyString());
        verify(client, never()).lookupProject(anyString(), anyString());
        
        assertThatCode(() -> uut.perform(build, workDir, launcher, listener)).isInstanceOf(AbortException.class).hasMessage("Dependency Track server upload failed");
    }

    @Test
    public void testPerformAsyncWithoutProjectId() throws IOException {
        File tmp = tmpDir.newFile();
        FilePath workDir = new FilePath(tmpDir.getRoot());
        final DependencyTrackPublisher uut = new DependencyTrackPublisher(tmp.getName(), false, clientFactory);
        uut.setProjectName("name-1");
        uut.setProjectVersion("version-1");
        
        when(client.upload(isNull(), eq("name-1"), eq("version-1"), any(FilePath.class), eq(false))).thenReturn(new UploadResult(true, "token-1"));
        
        assertThatCode(() -> uut.perform(build, workDir, launcher, listener)).doesNotThrowAnyException();
        verify(client, never()).lookupProject(anyString(), anyString());
        verify(client, never()).getFindings(anyString());
    }

    @Test
    public void testPerformSync() throws IOException {
        File tmp = tmpDir.newFile();
        FilePath workDir = new FilePath(tmpDir.getRoot());
        DependencyTrackPublisher uut = new DependencyTrackPublisher(tmp.getName(), true, clientFactory);
        uut.setProjectId("uuid-1");
        
        when(client.upload(eq("uuid-1"), isNull(), isNull(), any(FilePath.class), eq(false))).thenReturn(new UploadResult(true, "token-1"));
        when(client.isTokenBeingProcessed(eq("token-1"))).thenReturn(Boolean.TRUE).thenReturn(Boolean.FALSE);
        when(client.getFindings(eq("uuid-1"))).thenReturn(Collections.emptyList());
        
        assertThatCode(() -> uut.perform(build, workDir, launcher, listener)).doesNotThrowAnyException();
        verify(client, times(2)).isTokenBeingProcessed(eq("token-1"));
        verify(client).getFindings(eq("uuid-1"));
    }
    
}
