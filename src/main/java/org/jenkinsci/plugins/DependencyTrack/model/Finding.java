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

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Finding implements Serializable {

    private static final long serialVersionUID = 5309487290800777874L;

    Component component;
    Vulnerability vulnerability;
    Analysis analysis;

    // includes uuid of project, component and vulnerability delimited by colon
    @EqualsAndHashCode.Include
    String matrix;

    /**
     * checks whether this finding is an alias of the given other finding
     *
     * @param other the other finding to check against
     * @return {@code true} if the finding {@code other} is for the same
     * {@link #component} as this one and this
     * {@link #vulnerability} {@link Vulnerability#isAliasOf(org.jenkinsci.plugins.DependencyTrack.model.Vulnerability) is an alias of the other one}
     */
    public boolean isAliasOf(@NonNull final Finding other) {
        return vulnerability != null && component.equals(other.component) && other.getVulnerability() != null && vulnerability.isAliasOf(other.getVulnerability());
    }

    /**
     * checks whether the given other finding is an alias of this finding
     *
     * @param alias the possible alias to check
     * @return {@code true} if the finding {@code alias} is for the same
     * {@link #component} as this one and this
     * {@link #vulnerability} {@link Vulnerability#hasAlias(org.jenkinsci.plugins.DependencyTrack.model.Vulnerability) has the others one's vulnerability as an alias}
     */
    public boolean hasAlias(@NonNull final Finding alias) {
        return vulnerability != null && component.equals(alias.component) && alias.getVulnerability() != null && vulnerability.hasAlias(alias.getVulnerability());
    }

}
