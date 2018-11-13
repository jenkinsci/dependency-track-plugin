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

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import net.sf.json.JSONArray;
import org.jenkinsci.plugins.DependencyTrack.model.SeverityDistribution;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import java.util.ArrayList;
import java.util.List;

public class JobAction implements Action {

    private AbstractProject<?, ?> project;

    public JobAction(final AbstractProject<?, ?> project) {
        this.project = project;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return "dtrackTrend";
    }

    public AbstractProject<?, ?> getProject() {
        return this.project;
    }

    /**
     * Returns the UI model for an ECharts line chart that shows the issues stacked by severity.
     *
     * @return the UI model as JSON
     */
    @JavaScriptMethod
    @SuppressWarnings("unused") // Called by jelly view
    public JSONArray getSeverityDistributionTrend() {
        final List<SeverityDistribution> severityDistributions = new ArrayList<>();
        final List<? extends AbstractBuild<?, ?>> builds = project.getBuilds();
        int count = 0;
        for (AbstractBuild<?, ?> currentBuild : builds) {
            ResultAction action = currentBuild.getAction(ResultAction.class);
            if (action != null) {
                if (action.getSeverityDistribution() != null) {
                    severityDistributions.add(action.getSeverityDistribution());
                }
            }
            count++;
            if (count == 10) { // Only chart the last 10 builds (max)
                break;
            }
        }
        return JSONArray.fromObject(severityDistributions);
    }
}