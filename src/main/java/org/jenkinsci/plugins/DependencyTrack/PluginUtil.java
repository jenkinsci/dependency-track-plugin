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

import hudson.util.FormValidation;
import io.jenkins.plugins.okhttp.api.JenkinsOkHttpClient;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import lombok.experimental.UtilityClass;
import okhttp3.OkHttpClient;

import static org.jenkinsci.plugins.DependencyTrack.model.Permissions.*;

@UtilityClass
class PluginUtil {

    /**
     * Performs input validation when submitting the global config
     *
     * @param value The value of the URL as specified in the global config
     * @return a FormValidation object
     */
    @Nonnull
    static FormValidation doCheckUrl(@Nullable final String value) {
        if (isBlank(value)) {
            return FormValidation.ok();
        }
        try {
            URL url = URI.create(value).toURL();
            if (!url.getProtocol().toLowerCase().matches("https?")) {
                return FormValidation.error(Messages.Publisher_ConnectionTest_InvalidProtocols());
            }
        } catch (MalformedURLException | IllegalArgumentException e) {
            return FormValidation.error(Messages.Publisher_ConnectionTest_UrlMalformed());
        }
        return FormValidation.ok();
    }

    @Nullable
    static String parseBaseUrl(@Nullable final String baseUrl) {
        final var trimmed = trimToNull(baseUrl);
        return trimmed != null && trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
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
    static boolean areAllElementsOfType(@Nonnull final Collection<?> coll, @Nonnull final Class<?> type) {
        return coll.stream().allMatch(type::isInstance);
    }

    @Nonnull
    static OkHttpClient newHttpClient(final int connectionTimeout, final int readTimeout) {
        return JenkinsOkHttpClient.newClientBuilder(new OkHttpClient())
                .connectTimeout(Duration.ofSeconds(connectionTimeout))
                .readTimeout(Duration.ofSeconds(readTimeout))
                .build();
    }

    static boolean isBlank(@Nullable final String value) {
        return value == null || value.isBlank();
    }

    @Nullable
    static String trimToNull(@Nullable final String value) {
        return Optional.ofNullable(value)
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .orElse(null);
    }

    /**
     * creates the set of required permissions depending on the given options.
     *
     * @param synchronous value for synchronous publishing as specified in the
     * job config
     * @param projectProperties value for setting project properties as
     * @return set of required permissions
     */
    static Set<String> buildRequiredPermissions(final boolean synchronous, final boolean projectProperties) {
        final Set<String> requiredPermissions = HashSet.newHashSet(6);
        requiredPermissions.add(BOM_UPLOAD.toString());
        requiredPermissions.add(VIEW_PORTFOLIO.toString());
        requiredPermissions.add(VULNERABILITY_ANALYSIS.toString());
        if (synchronous) {
            requiredPermissions.add(VIEW_VULNERABILITY.toString());
            requiredPermissions.add(VIEW_POLICY_VIOLATION.toString());
        }
        if (projectProperties) {
            requiredPermissions.add(PORTFOLIO_MANAGEMENT.toString());
        }
        return requiredPermissions;
    }


    /**
     * creates the set of optional permissions depending on the given options.
     *
     * @param synchronous value for synchronous publishing as specified in the
     * job config
     * @param projectProperties value for setting project properties as
     * @return set of optional permissions
     */
    static Set<String> buildOptionalPermissions(final boolean synchronous, final boolean projectProperties) {
        final Set<String> optionalPermissions = HashSet.newHashSet(4);
        optionalPermissions.add(PROJECT_CREATION_UPLOAD.toString());
        if (!synchronous) {
            optionalPermissions.add(VIEW_VULNERABILITY.toString());
            optionalPermissions.add(VIEW_POLICY_VIOLATION.toString());
        }
        if (!projectProperties) {
            optionalPermissions.add(PORTFOLIO_MANAGEMENT.toString());
        }
        return optionalPermissions;
    }
}
