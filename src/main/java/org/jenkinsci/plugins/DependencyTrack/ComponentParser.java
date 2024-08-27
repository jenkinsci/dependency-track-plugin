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

import lombok.experimental.UtilityClass;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.DependencyTrack.model.Component;

/**
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
@UtilityClass
class ComponentParser extends ModelParser {

    Component parseComponent(JSONObject json) {
        final String uuid = getKeyOrNull(json, "uuid");
        final String name = getKeyOrNull(json, "name");
        final String group = getKeyOrNull(json, "group");
        final String version = getKeyOrNull(json, "version");
        final String purl = getKeyOrNull(json, "purl");
        return new Component(uuid, name, group, version, purl);
    }
}
