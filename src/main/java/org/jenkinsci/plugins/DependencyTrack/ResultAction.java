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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.jenkinsci.plugins.DependencyTrack.JasperReports.*;


import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.DependencyTrack.model.Finding;
import org.jenkinsci.plugins.DependencyTrack.model.SeverityDistribution;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.WebApp;
import org.kohsuke.stapler.WebMethod;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Plugin;
import hudson.PluginWrapper;
import hudson.model.Action;
import hudson.model.Run;
import hudson.util.HttpResponses;
import jenkins.model.Jenkins;
import jenkins.model.RunAction2;
import jenkins.tasks.SimpleBuildStep;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.sf.jasperreports.engine.JRException;
import net.sf.json.JSONArray;

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
     * the subtitle of the report on the report header
     */
    private String reportSubTitle;

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

    public String getBindUrl() {
        return WebApp.getCurrent().boundObjectTable.bind(this).getURL();
    }

    public String getCrumb() {
        return WebApp.getCurrent().getCrumbIssuer().issueCrumb();
    }
    
    /**
     * Set default if subtitle is not specified
     * @param aReportSubTitle
     */
    public void setReportSubTitle(String aReportSubTitle) {
    	 this.reportSubTitle = Optional.ofNullable(aReportSubTitle).orElse("summary report");
    	
	}

    @WebMethod(name="dt_summary.pdf")
    public HttpResponse doSummaryReportPdf() throws IOException, JRException {
        // return PDF report
		return HttpResponses.staticResource(new File(run.getRootDir(), PDF_REPORT_FILENAME));
    }

    @WebMethod(name="dt_summary.xlsx")
    public HttpResponse doSummaryReportExcel() throws IOException, JRException {
        // return CSV report
		return HttpResponses.staticResource(new File(run.getRootDir(), XLSX_REPORT_FILENAME));
    }

}
