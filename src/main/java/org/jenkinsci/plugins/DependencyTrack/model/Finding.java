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
package org.jenkinsci.plugins.DependencyTrack.model;

import java.io.Serializable;

public class Finding implements Serializable {

    private static final long serialVersionUID = 5309487290800777874L;

    private final Component component;
    private final Vulnerability vulnerability;
    private final Analysis analysis;
    private final String matrix;

    public Finding(Component component, Vulnerability vulnerability, Analysis analysis, String matrix) {
        this.component = component;
        this.vulnerability = vulnerability;
        this.analysis = analysis;
        this.matrix = matrix;
    }

    public Component getComponent() {
        return component;
    }

    public Vulnerability getVulnerability() {
        return vulnerability;
    }

    public Analysis getAnalysis() {
        return analysis;
    }

    public String getMatrix() {
        return matrix;
    }
}
