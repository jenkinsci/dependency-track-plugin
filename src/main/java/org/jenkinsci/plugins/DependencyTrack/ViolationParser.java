/*
 * Copyright 2024 OWASP.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.DependencyTrack.model.Violation;
import org.jenkinsci.plugins.DependencyTrack.model.ViolationState;
import org.jenkinsci.plugins.DependencyTrack.model.ViolationType;

@UtilityClass
class ViolationParser extends ModelParser {

    List<Violation> parse(final String jsonResponse) {
        final JSONArray jsonArray = JSONArray.fromObject(jsonResponse);
        return jsonArray.stream()
                .map(JSONObject.class::cast)
                .map(ViolationParser::parseViolation)
                .collect(Collectors.toList());
    }

    private Violation parseViolation(JSONObject json) {
        final var uuid = getKeyOrNull(json, "uuid");
        final var type = getEnum(json, "type", ViolationType.class);
        final var policy = json.getJSONObject("policyCondition").getJSONObject("policy");
        final var state = getEnum(policy, "violationState", ViolationState.class);
        final var policyName = getKeyOrNull(policy, "name");
        final var component = ComponentParser.parseComponent(json.getJSONObject("component"));
        return new Violation(uuid, type, state, policyName, component);
    }

}
