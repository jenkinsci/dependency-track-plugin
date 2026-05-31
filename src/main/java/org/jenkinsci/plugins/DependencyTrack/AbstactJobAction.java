/*
 * Copyright 2026 OWASP.
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
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.kohsuke.stapler.WebApp;

/**
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract sealed class AbstactJobAction extends InvisibleAction permits JobAction, ViolationsJobAction {

    @Override
    public final String getUrlName() {
        return "dtrackTrend";
    }

    public abstract boolean isTrendVisible();

    public final String getBindUrl() {
        return WebApp.getCurrent().boundObjectTable.bind(this).getURL();
    }

    public final String getCrumb() {
        return WebApp.getCurrent().getCrumbIssuer().issueCrumb();
    }
}
