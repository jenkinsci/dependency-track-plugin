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
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.sf.json.JSONArray;
import org.jenkinsci.plugins.DependencyTrack.model.Finding;
import org.jenkinsci.plugins.DependencyTrack.model.SeverityDistribution;
import org.kohsuke.stapler.bind.JavaScriptMethod;

/**
 * Action for storing the result of vulnerability findings.
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@RequiredArgsConstructor
public final class ResultAction extends AbstractRunAction implements Serializable {

    private static final long serialVersionUID = 9144544646132489130L;

    private final List<Finding> findings;
    private final SeverityDistribution severityDistribution;

    @Override
    public String getDisplayName() {
        return Messages.Result_DT_Report(getNameOrId());
    }

    @Override
    public String getUrlName() {
        return "dependency-track-findings";
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        return Set.of(new JobAction(run.getParent()));
    }

    public boolean hasFindings() {
        return findings != null && !findings.isEmpty();
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

}
