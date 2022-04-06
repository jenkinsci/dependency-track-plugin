/*
 * Copyright 2022 OWASP.
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

import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.DependencyTrack.model.Team;

/**
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
@UtilityClass
class TeamParser {

    Team parse(JSONObject json) {
        final Set<String> permissions = json.getJSONArray("permissions").stream()
                .map(JSONObject.class::cast)
                .map(o -> o.getString("name"))
                .collect(Collectors.toSet());
        
        return Team.builder()
                .name(json.getString("name"))
                .permissions(permissions)
                .build();
    }
}
