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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Plugin;
import hudson.PluginWrapper;
import hudson.model.Action;
import hudson.model.Run;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import jenkins.model.Jenkins;
import jenkins.model.RunAction2;
import jenkins.tasks.SimpleBuildStep;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.sf.json.JSONArray;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.DependencyTrack.model.Violation;
import org.kohsuke.stapler.WebApp;
import org.kohsuke.stapler.bind.JavaScriptMethod;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public class ViolationsRunAction implements RunAction2, SimpleBuildStep.LastBuildAction, Serializable {

    private static final long serialVersionUID = 8223620580665511318L;

    private transient Run<?, ?> run; // transient: see RunAction2, and JENKINS-45892
    private final List<Violation> violations;

    /**
     * the URL of the Dependency-Track Server to which these results are
     * belonging to
     */
    @Setter
    private String dependencyTrackUrl;

    /**
     * the ID of the project to which these results are belonging to
     */
    @Setter
    private String projectId;

    @Override
    public String getIconFileName() {
        return "/plugin/dependency-track/icons/dt-logo-symbol.svg";
    }

    @Override
    public String getDisplayName() {
        return Messages.Result_DT_ReportViolations();
    }

    @Override
    public String getUrlName() {
        return "dependency-track-violations";
    }

    @Override
    public void onAttached(Run<?, ?> run) {
        this.run = run;
    }

    @Override
    public void onLoad(Run<?, ?> run) {
        this.run = run;
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        return Set.of(new ViolationsJobAction(run.getParent()));
    }

    @NonNull
    public String getVersionHash() {
        return DigestUtils.sha256Hex(
                Optional.ofNullable(Jenkins.get().getPlugin("dependency-track"))
                        .map(Plugin::getWrapper)
                        .map(PluginWrapper::getVersion)
                        .orElse(StringUtils.EMPTY)
        );
    }

    public boolean hasViolations() {
        return violations != null && !violations.isEmpty();
    }

    /**
     * Returns the UI model for an ECharts line chart that shows the violations.
     *
     * @return the UI model as JSON
     */
    @JavaScriptMethod
    public JSONArray getViolationsJson() {
        run.checkPermission(hudson.model.Item.READ);
        return JSONArray.fromObject(violations);
    }

    public String getBindUrl() {
        return WebApp.getCurrent().boundObjectTable.bind(this).getURL();
    }

    public String getCrumb() {
        return WebApp.getCurrent().getCrumbIssuer().issueCrumb();
    }

}
