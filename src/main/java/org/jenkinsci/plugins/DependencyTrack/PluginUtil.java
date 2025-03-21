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

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.util.FormValidation;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang.StringUtils;

@UtilityClass
class PluginUtil {

    /**
     * Performs input validation when submitting the global config
     *
     * @param value The value of the URL as specified in the global config
     * @return a FormValidation object
     */
    @NonNull
    static FormValidation doCheckUrl(@Nullable final String value) {
        if (value == null || value.isBlank()) {
            return FormValidation.ok();
        }
        try {
            URL url = new URL(value);
            if (!url.getProtocol().toLowerCase().matches("https?")) {
                return FormValidation.error(Messages.Publisher_ConnectionTest_InvalidProtocols());
            }
        } catch (MalformedURLException e) {
            return FormValidation.error(Messages.Publisher_ConnectionTest_UrlMalformed());
        }
        return FormValidation.ok();
    }

    @Nullable
    static String parseBaseUrl(@Nullable final String baseUrl) {
        return StringUtils.removeEnd(StringUtils.trimToNull(baseUrl), "/");
    }

    /**
     * Checks if all elements of the given collection {@code coll} are of type
     * {@code type}
     *
     * @param coll the collection to check
     * @param type the class which the collection's elements are expected to be
     * @return {@code true} if all elements are of type {@code type} (also if
     * coll is empty), else {@code false}
     */
    static boolean areAllElementsOfType(@NonNull final Collection<?> coll, @NonNull final Class<?> type) {
        return coll.stream().allMatch(type::isInstance);
    }
}
