/*
 * This file is part of Dependency-Track Jenkins plugin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
import org.jenkinsci.plugins.DependencyTrack.model.Finding;
import org.jenkinsci.plugins.DependencyTrack.model.SeverityDistribution;
import org.kohsuke.stapler.WebApp;
import org.kohsuke.stapler.bind.JavaScriptMethod;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public class ResultAction implements RunAction2, SimpleBuildStep.LastBuildAction, Serializable {

    private static final long serialVersionUID = 9144544646132489130L;

    private transient Run<?, ?> run; // transient: see RunAction2, and JENKINS-45892
    private final List<Finding> findings;
    private final SeverityDistribution severityDistribution;

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
        return Messages.Result_DT_Report();
    }

    @Override
    public String getUrlName() {
        return "dependency-track-findings";
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
        return Collections.singleton(new JobAction(run.getParent()));
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

    /**
     * Returns the UI model for an ECharts line chart that shows the findings.
     *
     * @return the UI model as JSON
     */
    @JavaScriptMethod
    public JSONArray getFindingsJson() {
        run.checkPermission(hudson.model.Item.READ);
        return JSONArray.fromObject(findings);
    }

    public String getBindUrl() {
        return WebApp.getCurrent().boundObjectTable.bind(this).getURL();
    }

}
