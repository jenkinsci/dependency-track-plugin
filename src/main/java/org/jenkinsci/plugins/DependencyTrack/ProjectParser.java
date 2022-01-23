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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.DependencyTrack.model.Project;

@UtilityClass
class ProjectParser {

    Project parse(JSONObject json) {
        final String lastInheritedRiskScoreStr = getKeyOrNull(json, "lastInheritedRiskScore");
        final String activeStr = getKeyOrNull(json, "active");
        return Project.builder()
                .name(getKeyOrNull(json, "name"))
                .description(getKeyOrNull(json, "description"))
                .version(getKeyOrNull(json, "version"))
                .uuid(getKeyOrNull(json, "uuid"))
                .tags(json.has("tags") ? parseTags(json.getJSONArray("tags")) : Collections.emptyList())
                .lastBomImport(parseDateTime(getKeyOrNull(json, "lastBomImportStr")))
                .lastBomImportFormat(getKeyOrNull(json, "lastBomImportFormat"))
                .lastInheritedRiskScore(lastInheritedRiskScoreStr != null ? Double.parseDouble(lastInheritedRiskScoreStr) : null)
                .active(activeStr != null ? Boolean.parseBoolean(activeStr) : null)
                .swidTagId(getKeyOrNull(json, "swidTagId"))
                .group(getKeyOrNull(json, "group"))
                .build();
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

    private LocalDateTime parseDateTime(String dateTime) {
        if (dateTime == null) {
            return null;
        } else {
            return LocalDateTime.parse(dateTime);
        }
    }

    private List<String> parseTags(JSONArray tagArray) {
        return tagArray.stream()
                .map(o -> getKeyOrNull((JSONObject) o, "name"))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }
}
