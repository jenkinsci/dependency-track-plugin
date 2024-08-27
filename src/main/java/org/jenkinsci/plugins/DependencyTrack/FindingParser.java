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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.DependencyTrack.model.Analysis;
import org.jenkinsci.plugins.DependencyTrack.model.Component;
import org.jenkinsci.plugins.DependencyTrack.model.Finding;
import org.jenkinsci.plugins.DependencyTrack.model.Severity;
import org.jenkinsci.plugins.DependencyTrack.model.Vulnerability;

@UtilityClass
class FindingParser extends ModelParser {

    List<Finding> parse(final String jsonResponse) {
        final JSONArray jsonArray = JSONArray.fromObject(jsonResponse);
        return jsonArray.stream()
                .map(JSONObject.class::cast)
                .map(FindingParser::parseFinding)
                .collect(ArrayList<Finding>::new, (findings, finding) -> {
                    // filter duplicates based on aliases
                    // add if is not already included and if it is not an alias of an already present finding/vulnerability
                    if (!findings.contains(finding) && findings.stream().noneMatch(finding::isAliasOf)) {
                        findings.add(finding);
                    }
                }, List::addAll);
    }

    private Finding parseFinding(JSONObject json) {
        final Component component = ComponentParser.parseComponent(json.getJSONObject("component"));
        final Vulnerability vulnerability = parseVulnerability(json.getJSONObject("vulnerability"));
        final Analysis analysis = parseAnalysis(json.optJSONObject("analysis"));
        final String matrix = getKeyOrNull(json, "matrix");
        return new Finding(component, vulnerability, analysis, matrix);
    }

    private Vulnerability parseVulnerability(JSONObject json) {
        final String uuid = getKeyOrNull(json, "uuid");
        final String source = getKeyOrNull(json, "source");
        final String vulnId = getKeyOrNull(json, "vulnId");
        final String title = getKeyOrNull(json, "title");
        final String subtitle = getKeyOrNull(json, "subtitle");
        final String description = getKeyOrNull(json, "description");
        final String recommendation = getKeyOrNull(json, "recommendation");
        final Severity severity = getEnum(json, "severity", Severity.class);
        final Integer severityRank = json.optInt("severityRank");
        final var cwe = Optional.ofNullable(json.optJSONArray("cwes")).map(a -> a.optJSONObject(0)).filter(Predicate.not(JSONNull.class::isInstance));
        final Integer cweId = cwe.map(o -> o.optInt("cweId")).orElse(null);
        final String cweName = cwe.map(o -> getKeyOrNull(o, "name")).orElse(null);
        final var aliases = parseAliases(json, vulnId);
        return new Vulnerability(uuid, source, vulnId, title, subtitle, description, recommendation, severity, severityRank, cweId, cweName, aliases);
    }

    private Analysis parseAnalysis(JSONObject json) {
        final String state = getKeyOrNull(json, "state");
        final boolean isSuppressed = json.optBoolean("isSuppressed", false);
        return new Analysis(state, isSuppressed);
    }

    private List<String> parseAliases(JSONObject json, String vulnId) {
        final var aliases = json.optJSONArray("aliases");
        return aliases != null ? aliases.stream()
                .map(JSONObject.class::cast)
                .flatMap(alias -> alias.names().stream()
                .map(String.class::cast)
                .map(alias::getString)
                .filter(Predicate.not(vulnId::equalsIgnoreCase)))
                .distinct()
                .collect(Collectors.toList())
                : null;
    }
}
