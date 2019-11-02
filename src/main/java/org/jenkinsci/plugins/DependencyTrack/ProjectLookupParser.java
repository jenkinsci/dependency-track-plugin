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
import org.jenkinsci.plugins.DependencyTrack.model.Project;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProjectLookupParser {

    private final String jsonResponse;

    private Project project;

    public ProjectLookupParser(String jsonResponse) {
        this.jsonResponse = jsonResponse;
    }

    public ProjectLookupParser parse() {
        this.project = parseProject(JSONObject.fromObject(jsonResponse));
        return this;
    }

    private Project parseProject(JSONObject json) {
        final String name = getKeyOrNull(json, "name");
        final String description = getKeyOrNull(json, "description");
        final String version = getKeyOrNull(json, "version");
        final String uuid = getKeyOrNull(json, "uuid");
        final List<String> tags = json.has("tags") ? parseTags(json.getJSONArray("tags")) : Collections.emptyList();
        final LocalDateTime lastBomImport = parseDateTime(getKeyOrNull(json, "lastBomImportStr"));
        final String lastBomImportFormat = getKeyOrNull(json, "lastBomImportFormat");
        final String lastInheritedRiskScoreStr = getKeyOrNull(json, "lastInheritedRiskScore");
        final Double lastInheritedRiskScore = lastInheritedRiskScoreStr != null ? Double.parseDouble(lastInheritedRiskScoreStr) : null;
        final String activeStr = getKeyOrNull(json, "active");
        final Boolean active = activeStr != null ? Boolean.parseBoolean(activeStr) : null;
        return new Project(name, description, version, uuid, tags, lastBomImport, lastBomImportFormat, lastInheritedRiskScore, active);
    }

    private String getKeyOrNull(JSONObject json, String key) {
        return json.has(key) ? StringUtils.trimToNull(json.getString(key)) : null;
    }

    private LocalDateTime parseDateTime(String dateTime) {
        if (dateTime == null) {
            return null;
        } else {
            return LocalDateTime.parse(dateTime);
        }
    }

    private List<String> parseTags(JSONArray tagArray) {
        final List<String> tags = new ArrayList<>(tagArray.size());
        for (int i = 0; i < tagArray.size(); i++) {
            JSONObject tag = tagArray.getJSONObject(i);
            tags.add(tag.getString("name"));
        }
        return tags;
    }

    public Project getProject() {
        return project;
    }
}
