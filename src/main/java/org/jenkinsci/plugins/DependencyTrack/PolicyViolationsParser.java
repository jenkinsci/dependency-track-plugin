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

import lombok.experimental.UtilityClass;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.DependencyTrack.model.*;

import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
class PolicyViolationsParser
{
    List<PolicyViolation> parse(final String jsonResponse)
    {
        final JSONArray jsonArray = JSONArray.fromObject(jsonResponse);

		return
            jsonArray
                    .stream()
                    .map(o -> parse((JSONObject) o))
                    .collect(Collectors.toList());
    }

    private PolicyViolation parse(final JSONObject json)
    {
        return
                PolicyViolation.
                        of(
                                ParserUtil.getKeyOrNull(json, "uuid"),
                                ParserUtil.getEnumValue(Type.class, json, "type"),
                                ParserUtil.parseComponent(json.getJSONObject("component")),
                                parsePolicyCondition(json.getJSONObject("policyCondition")));
    }

    private PolicyCondition parsePolicyCondition(final JSONObject json)
    {
        return
                PolicyCondition
                        .of(
                                ParserUtil.getKeyOrNull(json, "uuid"),
                                parsePolicy(json.getJSONObject("policy")));
    }

    private Policy parsePolicy(final JSONObject json)
    {
        return
                Policy
                        .of(
                                ParserUtil.getKeyOrNull(json, "uuid"),
                                ParserUtil.getKeyOrNull(json, "name"),
                                ParserUtil.getEnumValue(ViolationState.class, json, "violationState"));
    }
}
