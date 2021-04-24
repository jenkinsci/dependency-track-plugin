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

import hudson.model.InvisibleAction;
import hudson.model.Job;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.sf.json.JSONArray;
import org.jenkinsci.plugins.DependencyTrack.model.SeverityDistribution;
import org.kohsuke.stapler.bind.JavaScriptMethod;

@RequiredArgsConstructor
public class JobAction extends InvisibleAction {

    @Getter
    @NonNull
    private final Job<?, ?> project;

    @Override
    public String getUrlName() {
        return "dtrackTrend";
    }

    /**
     * Returns whether the trend chart is visible or not.
     *
     * @return {@code true} if the trend is visible, false otherwise
     */
    public boolean isTrendVisible() {
        return project.getBuilds().stream()
                .map(run -> run.getAction(ResultAction.class))
                .anyMatch(Objects::nonNull);
    }

    /**
     * Returns the UI model for an ECharts line chart that shows the issues stacked by severity.
     *
     * @return the UI model as JSON
     */
    @JavaScriptMethod
    public JSONArray getSeverityDistributionTrend() {
        project.checkPermission(hudson.model.Item.READ);
        final List<SeverityDistribution> severityDistributions = project.getBuilds().stream()
                .sorted(Comparator.naturalOrder())
                .map(run -> run.getAction(ResultAction.class)).filter(Objects::nonNull)
                .map(ResultAction::getSeverityDistribution)
                .collect(Collectors.toList());
        return JSONArray.fromObject(severityDistributions);
    }
}