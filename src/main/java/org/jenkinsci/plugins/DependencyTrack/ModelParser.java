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

import java.util.Optional;
import java.util.function.Predicate;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONUtils;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
abstract class ModelParser {

    protected ModelParser() {
    }

    protected static final String getKeyOrNull(final JSONObject json, final String key) {
        // key can be null. but it may also be JSONNull!
        // optString and getString do not check if v is JSONNull. instead they return just v.toString() which will be "null"!
        return Optional.ofNullable(json.opt(key))
                .filter(Predicate.not(JSONUtils::isNull))
                .map(Object::toString)
                .map(StringUtils::trimToNull)
                .orElse(null);
    }

    protected static final <T extends Enum<T>> T getEnum(final JSONObject json, final String key, final Class<T> enumType) {
        final var value = getKeyOrNull(json, key);
        try {
            return value != null ? Enum.valueOf(enumType, value) : null;
        } catch (IllegalArgumentException ignore) {
            return null;
        }
    }
}
