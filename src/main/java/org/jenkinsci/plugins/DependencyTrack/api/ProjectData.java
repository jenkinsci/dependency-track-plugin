/*
 * Copyright 2025 OWASP.
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
package org.jenkinsci.plugins.DependencyTrack.api;

import jakarta.annotation.Nullable;
import java.util.List;

/**
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
public final record ProjectData(@Nullable String id,
        @Nullable String name,
        @Nullable String version,
        boolean autoCreate,
        @Nullable Properties properties) {

    public static final record Properties(
            @Nullable List<String> tags,
            @Nullable String swidTagId,
            @Nullable String group,
            @Nullable String description,
            @Nullable String parentId,
            @Nullable String parentName,
            @Nullable String parentVersion,
            @Nullable Boolean isLatest) {

    }
}
