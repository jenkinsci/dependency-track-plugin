/*
 * Copyright 2026 OWASP.
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

import hudson.Plugin;
import hudson.PluginWrapper;
import hudson.model.Run;
import jakarta.annotation.Nonnull;
import java.io.Serializable;
import java.util.Optional;
import jenkins.model.Jenkins;
import jenkins.model.RunAction2;
import jenkins.tasks.SimpleBuildStep;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.codec.digest.DigestUtils;
import org.kohsuke.stapler.WebApp;

/**
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract sealed class AbstractRunAction implements RunAction2, SimpleBuildStep.LastBuildAction, Serializable permits ResultAction, ViolationsRunAction {
    
    protected transient Run<?, ?> run; // transient: see RunAction2, and JENKINS-45892

    /**
     * the URL of the Dependency-Track Server to which these results are
     * belonging to
     */
    @Setter
    @EqualsAndHashCode.Include
    private String dependencyTrackUrl;

    /**
     * the ID of the project to which these results are belonging to
     */
    @Setter
    @EqualsAndHashCode.Include
    private String projectId;

    /**
     * the name of the project to which these results are belonging to
     */
    @Setter
    private String projectName;

    @Override
    public final String getIconFileName() {
        return "/plugin/dependency-track/icons/dt-logo-symbol.svg";
    }

    @Override
    public final void onAttached(Run<?, ?> run) {
        this.run = run;
    }

    @Override
    public final void onLoad(Run<?, ?> run) {
        this.run = run;
    }

    /**
     * returns the prpject name if it is not empty, or else the project id
     *
     * @return {@link #projectName} or {@link #projectId}
     */
    public final String getNameOrId() {
        return !PluginUtil.isBlank(projectName) ? projectName : projectId;
    }

    @Nonnull
    public final String getVersionHash() {
        return DigestUtils.sha256Hex(
                Optional.ofNullable(Jenkins.get().getPlugin("dependency-track"))
                        .map(Plugin::getWrapper)
                        .map(PluginWrapper::getVersion)
                        .orElse("")
        );
    }

    public final String getBindUrl() {
        return WebApp.getCurrent().boundObjectTable.bind(this).getURL();
    }

    public final String getCrumb() {
        return WebApp.getCurrent().getCrumbIssuer().issueCrumb();
    }
}
