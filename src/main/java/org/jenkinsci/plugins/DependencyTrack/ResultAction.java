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

import hudson.model.Action;
import hudson.model.Run;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import jenkins.model.RunAction2;
import jenkins.tasks.SimpleBuildStep;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.sf.json.JSONArray;
import org.jenkinsci.plugins.DependencyTrack.model.Finding;
import org.jenkinsci.plugins.DependencyTrack.model.SeverityDistribution;
import org.kohsuke.stapler.bind.JavaScriptMethod;

@Getter
@EqualsAndHashCode
public class ResultAction implements RunAction2, SimpleBuildStep.LastBuildAction, Serializable {
    
    private static final long serialVersionUID = 9144544646132489130L;

    private transient Run<?, ?> run; // transient: see RunAction2, and JENKINS-45892
    private final List<Finding> findings;
    private final SeverityDistribution severityDistribution;

    public ResultAction(Run<?, ?> build, List<Finding> findings, SeverityDistribution severityDistribution) {
        this.findings = findings;
        this.severityDistribution = severityDistribution;
    }

    @Override
    public String getIconFileName() {
        return "/plugin/dependency-track/icons/dt-logo-symbol.svg";
    }

    @Override
    public String getDisplayName() {
        return "Dependency-Track Report";
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

    /**
     * Returns the UI model for an ECharts line chart that shows the findings.
     *
     * @return the UI model as JSON
     */
    @JavaScriptMethod
    public JSONArray getFindingsJson() {
        return JSONArray.fromObject(findings);
    }

}
