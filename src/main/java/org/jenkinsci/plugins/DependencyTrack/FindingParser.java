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

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.DependencyTrack.model.Analysis;
import org.jenkinsci.plugins.DependencyTrack.model.Component;
import org.jenkinsci.plugins.DependencyTrack.model.Finding;
import org.jenkinsci.plugins.DependencyTrack.model.Severity;
import org.jenkinsci.plugins.DependencyTrack.model.SeverityDistribution;
import org.jenkinsci.plugins.DependencyTrack.model.Vulnerability;
import java.util.ArrayList;
import java.util.List;

public class FindingParser {

    private final String jsonResponse;
    private List<Finding> findings;
    private SeverityDistribution severityDistribution;

    public FindingParser(int buildNumber, String jsonResponse) {
        this.severityDistribution = new SeverityDistribution(buildNumber);
        this.jsonResponse = jsonResponse;
    }

    public FindingParser parse() {
        final List<Finding> findings = new ArrayList<>();
        final JSONArray jsonArray = JSONArray.fromObject(jsonResponse);
        for (int i = 0; i < jsonArray.size(); i++) {
            final Finding finding = parseFinding(jsonArray.getJSONObject(i));
            findings.add(finding);
        }
        this.findings = findings;
        return this;
    }

    private Finding parseFinding(JSONObject json) {
        final Component component = parseComponent(json.getJSONObject("component"));
        final Vulnerability vulnerability = parseVulnerability(json.getJSONObject("vulnerability"));
        final Analysis analysis = parseAnalysis(json.optJSONObject("analysis"));
        final String matrix = StringUtils.trimToNull(json.getString("matrix"));
        return new Finding(component, vulnerability, analysis, matrix);
    }

    private Component parseComponent(JSONObject json) {
        final String uuid = StringUtils.trimToNull(json.getString("uuid"));
        final String name = StringUtils.trimToNull(json.getString("name"));
        final String group = StringUtils.trimToNull(json.optString("group"));
        final String version = StringUtils.trimToNull(json.optString("version"));
        final String purl = StringUtils.trimToNull(json.optString("purl"));
        return new Component(uuid, name, group, version, purl);
    }

    private Vulnerability parseVulnerability(JSONObject json) {
        final String uuid = StringUtils.trimToNull(json.getString("uuid"));
        final String source = StringUtils.trimToNull(json.getString("source"));
        final String vulnId = StringUtils.trimToNull(json.optString("vulnId"));
        final String title = StringUtils.trimToNull(json.optString("title"));
        final String subtitle = StringUtils.trimToNull(json.optString("subtitle"));
        final String description = StringUtils.trimToNull(json.optString("description"));
        final String recommendation = StringUtils.trimToNull(json.optString("recommendation"));
        final Severity severity = Severity.valueOf(json.optString("severity"));
        final Integer severityRank = json.optInt("severityRank");
        final Integer cweId = json.optInt("cweId");
        final String cweName = StringUtils.trimToNull(json.optString("cweName"));
        severityDistribution.add(severity);
        return new Vulnerability(uuid, source, vulnId, title, subtitle, description, recommendation, severity, severityRank, cweId, cweName);
    }

    private Analysis parseAnalysis(JSONObject json) {
        final String state = StringUtils.trimToNull(json.optString("state"));
        final boolean isSuppressed = json.optBoolean("isSuppressed", false);
        return new Analysis(state, isSuppressed);
    }

    public List<Finding> getFindings() {
        return findings;
    }

    public SeverityDistribution getSeverityDistribution() {
        return severityDistribution;
    }
}
