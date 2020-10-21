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
package org.jenkinsci.plugins.DependencyTrack.transformer;

import java.util.List;
import java.util.Locale;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.DependencyTrack.model.Analysis;
import org.jenkinsci.plugins.DependencyTrack.model.Component;
import org.jenkinsci.plugins.DependencyTrack.model.Finding;
import org.jenkinsci.plugins.DependencyTrack.model.Severity;
import org.jenkinsci.plugins.DependencyTrack.model.Vulnerability;

/**
 * Converts a list of Findings into a data structure suitable
 * for the FooTable Javascript component.
 */
public class FindingsTransformer {

    public JSONObject transform(List<Finding> findings) {
        final JSONArray columns = new JSONArray();

        final JSONObject c1 = new JSONObject();
        c1.put("name", "component.nameLabel");
        c1.put("title", "Name");
        c1.put("visible", true);
        c1.put("filterable", true);
        c1.put("sortValue", "component.name");
        columns.add(c1);

        final JSONObject c2 = new JSONObject();
        c2.put("name", "component.versionLabel");
        c2.put("title", "Version");
        c2.put("visible", true);
        c2.put("filterable", true);
        c2.put("sortValue", "component.version");
        columns.add(c2);

        final JSONObject c3 = new JSONObject();
        c3.put("name", "component.groupLabel");
        c3.put("title", "Group");
        c3.put("visible", true);
        c3.put("filterable", true);
        c3.put("sortValue", "component.group");
        columns.add(c3);

        final JSONObject c4 = new JSONObject();
        c4.put("name", "vulnerability.vulnIdLabel");
        c4.put("title", "Vulnerability");
        c4.put("visible", true);
        c4.put("filterable", true);
        c4.put("sortValue", "vulnerability.vulnId");
        columns.add(c4);

        final JSONObject c5 = new JSONObject();
        c5.put("name", "vulnerability.severityLabel");
        c5.put("title", "Severity");
        c5.put("visible", true);
        c5.put("filterable", true);
        c5.put("sortValue", "vulnerability.severityRank");
        columns.add(c5);

        final JSONObject c6 = new JSONObject();
        c6.put("name", "vulnerability.cweLabel");
        c6.put("title", "CWE");
        c6.put("visible", true);
        c6.put("filterable", true);
        c6.put("sortValue", "vulnerability.cweId");
        JSONObject c61 = new JSONObject();
        c61.put("width", "30%");
        c6.put("style", c61);
        columns.add(c6);

        final JSONObject c7 = new JSONObject();
        c7.put("name", "vulnerability.title");
        c7.put("title", "Title");
        c7.put("breakpoints", "all");
        c7.put("visible", true);
        c7.put("filterable", false);
        columns.add(c7);

        final JSONObject c8 = new JSONObject();
        c8.put("name", "vulnerability.subtitle");
        c8.put("title", "Subtitle");
        c8.put("breakpoints", "all");
        c8.put("visible", true);
        c8.put("filterable", false);
        columns.add(c8);

        final JSONObject c9 = new JSONObject();
        c9.put("name", "vulnerability.description");
        c9.put("title", "Description");
        c9.put("breakpoints", "all");
        c9.put("visible", true);
        c9.put("filterable", false);
        columns.add(c9);

        final JSONObject c10 = new JSONObject();
        c10.put("name", "vulnerability.recommendation");
        c10.put("title", "Recommendation");
        c10.put("breakpoints", "all");
        c10.put("visible", true);
        c10.put("filterable", false);
        columns.add(c10);

        final JSONObject c11 = new JSONObject();
        c11.put("name", "analysis.state");
        c11.put("title", "Analysis");
        c11.put("breakpoints", "all");
        c11.put("visible", true);
        c11.put("filterable", false);
        columns.add(c11);

        final JSONArray rows = new JSONArray();
        for (Finding finding: findings) {
            final Component component = finding.getComponent();
            final Vulnerability vulnerability = finding.getVulnerability();
            final Analysis analysis = finding.getAnalysis();
            final JSONObject row = new JSONObject();
            row.put("component.uuid", component.getUuid());
            row.put("component.name", component.getName());
            row.put("component.nameLabel", component.getName());
            row.put("component.version", component.getVersion());
            row.put("component.versionLabel", component.getVersion());
            row.put("component.group", component.getGroup());
            row.put("component.groupLabel", component.getGroup());
            row.put("component.purl", component.getPurl());
            row.put("vulnerability.uuid", vulnerability.getUuid());
            row.put("vulnerability.source", vulnerability.getSource());
            row.put("vulnerability.vulnId", vulnerability.getVulnId());
            row.put("vulnerability.vulnIdLabel", generateVulnerabilityField(vulnerability.getSource(), vulnerability.getVulnId()));
            row.put("vulnerability.title", vulnerability.getTitle());
            row.put("vulnerability.subtitle", vulnerability.getSubtitle());
            row.put("vulnerability.description", vulnerability.getDescription());
            row.put("vulnerability.recommendation", vulnerability.getRecommendation());
            row.put("vulnerability.severityLabel", generateSeverityField(vulnerability.getSeverity()));
            row.put("vulnerability.severity", vulnerability.getSeverity());
            row.put("vulnerability.severityRank", vulnerability.getSeverityRank());
            row.put("vulnerability.cweLabel", generateCweField(vulnerability.getCweId(), vulnerability.getCweName()));
            row.put("vulnerability.cweId", vulnerability.getCweId());
            row.put("vulnerability.cweName", vulnerability.getCweName());
            row.put("analysis.state", analysis.getState());
            row.put("analysis.isSuppressed", analysis.isSuppressed());
            rows.add(row);
        }
        final JSONObject data = new JSONObject();
        data.put("columns", columns);
        data.put("rows", rows);
        return data;
    }

    private String generateSeverityField(Severity severity) {
        return "<div style=\"height:24px;margin:-4px;\">\n" +
                "<div class=\"severity-" + severity.name().toLowerCase() + "-bg text-center pull-left\" style=\"width:24px; height:24px; color:#ffffff\">\n" +
                "  <i class=\"fa fa-bug\" style=\"font-size:12px; padding:6px\" aria-hidden=\"true\"></i>\n" +
                "</div>\n" +
                "<div class=\"text-center pull-left\" style=\"height:24px;\">\n" +
                "  <div style=\"font-size:12px; padding:4px\"><span class=\"severity-value\">" + convert(severity.name()) + "</span></div>\n" +
                "</div>\n" +
                "</div>";
    }

    private String generateVulnerabilityField(String source, String vulnId) {
        return "<span class=\"vuln-source vuln-source-" + source.toLowerCase() + "\">" + source + "</span>" + vulnId;
    }

    private String generateCweField(Integer cweId, String cweName) {
        if (cweId == null || cweName == null) {
            return null;
        }
        return generateTruncatedStringField("CWE-" + cweId + " " + cweName);
    }

    private String generateTruncatedStringField(String in) {
        if (in == null) {
            return null;
        }
        return "<div class=\"truncate-ellipsis\"><span>" + in + "</span></div>";
    }

    private String convert(String str) {
        return StringUtils.capitalize(str.toLowerCase(Locale.ROOT));
    }
}
