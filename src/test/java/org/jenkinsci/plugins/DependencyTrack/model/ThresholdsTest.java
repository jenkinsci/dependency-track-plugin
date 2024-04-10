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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ThresholdsTest {

    @Test
    void hasValuesTest() {
        var uut = new Thresholds();
        assertThat(uut.hasValues()).isFalse();

        uut.newFindings.failedCritical = 1;
        assertThat(uut.hasValues()).isTrue();

        uut = new Thresholds();
        uut.newFindings.unstableUnassigned = 1;
        assertThat(uut.hasValues()).isTrue();

        uut = new Thresholds();
        uut.totalFindings.failedHigh = 1;
        assertThat(uut.hasValues()).isTrue();

        uut = new Thresholds();
        uut.totalFindings.unstableMedium = 1;
        assertThat(uut.hasValues()).isTrue();
    }
}
