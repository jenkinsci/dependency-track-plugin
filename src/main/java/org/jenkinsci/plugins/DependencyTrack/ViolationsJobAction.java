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
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.DependencyTrack.model.ViolationState;
import org.kohsuke.stapler.WebApp;
import org.kohsuke.stapler.bind.JavaScriptMethod;

/**
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
@RequiredArgsConstructor
public class ViolationsJobAction extends InvisibleAction {

    @Getter
    @NonNull
    private final Job<?, ?> project;

    @Override
    public String getUrlName() {
        return "dtrackTrend";
    }

    /**
     * Returns whether the policy violations trend chart is visible or not.
     *
     * @return {@code true} if the trend is visible, false otherwise
     */
    public boolean isTrendVisible() {
        return project.getBuilds().stream()
                .map(run -> run.getAction(ViolationsRunAction.class))
                .anyMatch(Objects::nonNull);
    }

    /**
     * Returns the UI model for an ECharts line chart that shows the violations
     * stacked by state.
     *
     * @return the UI model as JSON
     */
    @JavaScriptMethod
    public JSONArray getViolationsTrend() {
        project.checkPermission(hudson.model.Item.READ);
        final List<JSONObject> distributions = project.getBuilds().stream()
                .sorted(Comparator.naturalOrder())
                .map(run -> run.getAction(ViolationsRunAction.class)).filter(Objects::nonNull)
                .map(result -> {
                    final var violations = result.getViolations()
                            .stream()
                            .collect(Collectors.toMap(violation -> violation.getState().name().toLowerCase(), i -> 1, (a, b) -> a + b));
                    final var item = new JSONObject();
                    item.element("buildNumber", result.getRun().getNumber());
                    item.putAll(violations);
                    // ensure all keys exist
                    item.putIfAbsent(ViolationState.INFO.name().toLowerCase(), 0);
                    item.putIfAbsent(ViolationState.WARN.name().toLowerCase(), 0);
                    item.putIfAbsent(ViolationState.FAIL.name().toLowerCase(), 0);
                    return item;
                })
                .collect(Collectors.toList());
        return JSONArray.fromObject(distributions);
    }

    public String getBindUrl() {
        return WebApp.getCurrent().boundObjectTable.bind(this).getURL();
    }

    public String getCrumb() {
        return WebApp.getCurrent().getCrumbIssuer().issueCrumb();
    }
}
