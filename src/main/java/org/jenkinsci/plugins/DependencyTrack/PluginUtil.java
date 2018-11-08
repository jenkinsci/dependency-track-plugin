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

import hudson.FilePath;
import hudson.util.FormValidation;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

public class PluginUtil implements Serializable {

    private static final long serialVersionUID = 5780698407191725723L;

    private PluginUtil() { }

    /**
     * Performs input validation when submitting the global config
     * @param value The value of the URL as specified in the global config
     * @return a FormValidation object
     */
    static FormValidation doCheckUrl(@QueryParameter String value) {
        if (StringUtils.isBlank(value)) {
            return FormValidation.ok();
        }
        try {
            new URL(value);
        } catch (MalformedURLException e) {
            return FormValidation.error("The specified value is not a valid URL");
        }
        return FormValidation.ok();
    }

    /**
     * Performs input validation when submitting the global config
     * @param value The value of the path as specified in the global config
     * @return a FormValidation object
     */
    static FormValidation doCheckPath(@QueryParameter String value) {
        if (StringUtils.isBlank(value)) {
            return FormValidation.ok();
        }
        try {
            final FilePath filePath = new FilePath(new File(value));
            filePath.exists();
        } catch (Exception e) {
            return FormValidation.error("The specified value is not a valid path");
        }
        return FormValidation.ok();
    }

    static String parseBaseUrl(String baseUrl) {
        baseUrl = StringUtils.trimToNull(baseUrl);
        if (baseUrl != null && baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() -1);
        }
        return baseUrl;
    }
}
