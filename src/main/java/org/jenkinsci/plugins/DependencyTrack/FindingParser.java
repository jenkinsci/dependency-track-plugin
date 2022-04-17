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

import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.DependencyTrack.model.Analysis;
import org.jenkinsci.plugins.DependencyTrack.model.Component;
import org.jenkinsci.plugins.DependencyTrack.model.Finding;
import org.jenkinsci.plugins.DependencyTrack.model.Severity;
import org.jenkinsci.plugins.DependencyTrack.model.Vulnerability;

@UtilityClass
class FindingParser {

    List<Finding> parse(final String jsonResponse) {
        final JSONArray jsonArray = JSONArray.fromObject(jsonResponse);
		return jsonArray.stream()
                .map(o -> parseFinding((JSONObject) o))
                .collect(Collectors.toList());
    }

    private Finding parseFinding(JSONObject json) {
        final Component component = parseComponent(json.getJSONObject("component"));
        final Vulnerability vulnerability = parseVulnerability(json.getJSONObject("vulnerability"));
        final Analysis analysis = parseAnalysis(json.optJSONObject("analysis"));
        final String matrix = getKeyOrNull(json, "matrix");
        return new Finding(component, vulnerability, analysis, matrix);
    }

    private Component parseComponent(JSONObject json) {
        final String uuid = getKeyOrNull(json, "uuid");
        final String name = getKeyOrNull(json, "name");
        final String group = getKeyOrNull(json, "group");
        final String version = getKeyOrNull(json, "version");
        final String purl = getKeyOrNull(json, "purl");
        return new Component(uuid, name, group, version, purl);
    }

    private Vulnerability parseVulnerability(JSONObject json) {
        final String uuid = getKeyOrNull(json, "uuid");
        final String source = getKeyOrNull(json, "source");
        final String vulnId = getKeyOrNull(json, "vulnId");
        final String title = getKeyOrNull(json, "title");
        final String subtitle = getKeyOrNull(json, "subtitle");
        final String description = getKeyOrNull(json, "description");
        final String recommendation = getKeyOrNull(json, "recommendation");
        final Severity severity = Severity.valueOf(json.optString("severity"));
        final Integer severityRank = json.optInt("severityRank");
        final Integer cweId = json.optInt("cweId");
        final String cweName = getKeyOrNull(json, "cweName");
        return new Vulnerability(uuid, source, vulnId, title, subtitle, description, recommendation, severity, severityRank, cweId, cweName);
    }

    private Analysis parseAnalysis(JSONObject json) {
        final String state = getKeyOrNull(json, "state");
        final boolean isSuppressed = json.optBoolean("isSuppressed", false);
        return new Analysis(state, isSuppressed);
    }

    private String getKeyOrNull(JSONObject json, String key) {
        // key can be null. but it may also be JSONNull!
        // optString and getString do not check if v is JSONNull. instead they return just v.toString() which will be "null"!
        Object v = json.opt(key);
        if (v instanceof JSONNull) {
            v = null;
        }
        return v == null ? null : StringUtils.trimToNull(v.toString());
    }
}
