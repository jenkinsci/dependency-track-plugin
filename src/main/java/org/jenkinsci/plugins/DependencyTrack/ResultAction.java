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

import hudson.model.Run;
import jenkins.model.RunAction2;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import org.jenkinsci.plugins.DependencyTrack.model.Finding;
import org.jenkinsci.plugins.DependencyTrack.model.SeverityDistribution;
import org.jenkinsci.plugins.DependencyTrack.transformer.FindingsTransformer;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ResultAction implements RunAction2, Serializable {

    private static final long serialVersionUID = -3741322502842596768L;
    private transient Run run;
    private ArrayList<Finding> findings;
    private SeverityDistribution severityDistribution;

    public ResultAction(ArrayList<Finding> findings, SeverityDistribution severityDistribution) {
        this.findings = findings;
        this.severityDistribution = severityDistribution;
    }

    @Override
    public String getIconFileName() {
        return "/plugin/dependency-track/icons/dt-logo-symbol.svg";
    }

    @Override
    public String getDisplayName() {
        return "Dependency-Track";
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

    public Run getRun() {
        return run;
    }

    public SeverityDistribution getSeverityDistribution() {
        return severityDistribution;
    }

    public List<Finding> getFindings() {
        return findings;
    }

    /**
     * Returns the UI model for an ECharts line chart that shows the findings.
     *
     * @return the UI model as JSON
     */
    @JavaScriptMethod
    @SuppressWarnings("unused") // Called by jelly view
    public JSONObject getFindingsJson() {
        final FindingsTransformer transformer = new FindingsTransformer();
        return transformer.transform(findings);
    }

    /**
     * Returns a JSON response with the statistics for severity.
     *
     * @return the UI model as JSON
     */
    @JavaScriptMethod
    @SuppressWarnings("unused") // Called by jelly view
    public JSONObject getSeverityDistributionJson() {
        JsonConfig jsonConfig = new JsonConfig();
        jsonConfig.setExcludes( new String[]{ "buildNumber"} );
        return JSONObject.fromObject(severityDistribution, jsonConfig);
    }

}