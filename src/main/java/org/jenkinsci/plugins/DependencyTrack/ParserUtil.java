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

import lombok.experimental.UtilityClass;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.DependencyTrack.model.Component;

import javax.annotation.Nullable;

@UtilityClass
public class ParserUtil
{
  @Nullable
  String getKeyOrNull(JSONObject json, String key) {
    // key can be null. but it may also be JSONNull!
    // optString and getString do not check if v is JSONNull. instead they return just v.toString() which will be "null"!
    Object v = json.opt(key);
    if (v instanceof JSONNull) {
      v = null;
    }
    return v == null ? null : StringUtils.trimToNull(v.toString());
  }

  @Nullable
  <T extends Enum<T>> T getEnumValue(final Class<T> type,
                                             final JSONObject json,
                                             final String key)
  {
    final String value = getKeyOrNull(json, key);

    if (value != null)
    {
      return Enum.valueOf(type, value);
    }

    return null;
  }

  Component parseComponent(final JSONObject json)
  {
    return
            Component
                    .of(
                            getKeyOrNull(json, "uuid"),
                            getKeyOrNull(json, "name"),
                            getKeyOrNull(json, "group"),
                            getKeyOrNull(json, "version"),
                            getKeyOrNull(json, "purl"));
  }
}
