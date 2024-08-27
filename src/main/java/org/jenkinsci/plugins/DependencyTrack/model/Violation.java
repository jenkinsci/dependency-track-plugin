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
package org.jenkinsci.plugins.DependencyTrack.model;

import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Violation implements Serializable {

    private static final long serialVersionUID = -2591514706071774767L;

    @EqualsAndHashCode.Include
    private final String uuid;
    private final ViolationType type;
    private final ViolationState state;
    private final String policyName;
    private final Component component;

    public int getStateRank() {
        return state.ordinal();
    }
}
