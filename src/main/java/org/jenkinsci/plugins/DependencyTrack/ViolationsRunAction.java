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

import hudson.model.Action;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.sf.json.JSONArray;
import org.jenkinsci.plugins.DependencyTrack.model.Violation;
import org.kohsuke.stapler.bind.JavaScriptMethod;

/**
 * Action for storing the result of policy violations.
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@RequiredArgsConstructor
public final class ViolationsRunAction extends AbstractRunAction implements Serializable {

    private static final long serialVersionUID = 8223620580665511318L;

    private final List<Violation> violations;

    @Override
    public String getDisplayName() {
        return Messages.Result_DT_ReportViolations(getNameOrId());
    }

    @Override
    public String getUrlName() {
        return "dependency-track-violations";
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        return Set.of(new ViolationsJobAction(run.getParent()));
    }

    public boolean hasViolations() {
        return violations != null && !violations.isEmpty();
    }

    /**
     * Returns the UI model for an ECharts line chart that shows the violations.
     *
     * @return the UI model as JSON
     */
    @JavaScriptMethod
    public JSONArray getViolationsJson() {
        run.checkPermission(hudson.model.Item.READ);
        return JSONArray.fromObject(violations);
    }

}
