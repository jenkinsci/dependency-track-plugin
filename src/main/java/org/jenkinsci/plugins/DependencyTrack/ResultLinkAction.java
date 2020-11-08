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

import hudson.model.Run;
import java.io.Serializable;
import jenkins.model.RunAction2;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;

/**
 * Action to Provide a Link to the Project on the Dependency-Track Server
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public class ResultLinkAction implements RunAction2, Serializable {

    private static final long serialVersionUID = 9144463546984654654L;

    /**
     * the URL of the Dependency-Track Server to which these results are
     * belonging to
     */
    private final String dependencyTrackUrl;

    /**
     * the ID of the project to which these results are belonging to
     */
    private final String projectId;

    /**
     * the name of the project to which these results are belonging to
     */
    @Setter
    private String projectName;

    /**
     * the version of the project to which these results are belonging to
     */
    @Setter
    private String projectVersion;

    @Override
    public String getIconFileName() {
        return isEnabled() ? "/plugin/dependency-track/icons/dt-logo-symbol.svg" : null;
    }

    @Override
    public String getDisplayName() {
        return isEnabled() ? Messages.Result_DT_Project(): null;
    }

    @Override
    public String getUrlName() {
        return isEnabled() ? String.format("%s/projects/%s", dependencyTrackUrl, projectId) : null;
    }

    @Override
    public void onAttached(Run<?, ?> run) {
        // nothing to do
    }

    @Override
    public void onLoad(Run<?, ?> run) {
        // nothing to do
    }

    private boolean isEnabled() {
        return StringUtils.isNotBlank(dependencyTrackUrl) && StringUtils.isNotBlank(projectId);
    }

}
