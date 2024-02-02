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

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FindingTest {

    @Test
    void testAliases() {
        var a1 = new Analysis("state-1", false);
        var c1 = new Component("uuid-1", "name-1", "group-1", "version-1", "purl-1");

        var aliases1 = List.of("GHSA-abcd-abcd-abcd", "FOO-12345");
        var v1 = new Vulnerability("uuid-1", "NVD", "vulnId-1", "title-1", "subtitle-1", "description-1", "recommendation-1", Severity.CRITICAL, 1, 2, "cweName-1", aliases1);
        var f1 = new Finding(c1, v1, a1, "matrix-1");

        var aliases2 = List.of("vulnId-1", "FOO-12345");
        var v2 = new Vulnerability("uuid-2", "GITHUB", "GHSA-abcd-abcd-abcd", "title-1", "subtitle-1", "description-1", "recommendation-1", Severity.CRITICAL, 1, 2, "cweName-1", aliases2);
        var f2 = new Finding(c1, v2, a1, "matrix-2");

        assertThat(f2.isAliasOf(f1)).isTrue();
        assertThat(f1.isAliasOf(f2)).isTrue();
        assertThat(f1.hasAlias(f2)).isTrue();
        assertThat(f2.hasAlias(f1)).isTrue();

        var v3 = new Vulnerability("uuid-2", "GITHUB", "GHSA-abcd-abcd-abcd", "title-1", "subtitle-1", "description-1", "recommendation-1", Severity.CRITICAL, 1, 2, "cweName-1", null);
        var f3 = new Finding(c1, v3, a1, "matrix-2");
        
        var c2 = new Component("uuid-2", "name-1", "group-1", "version-1", "purl-1");
        var f4 = new Finding(c2, v1, a1, "matrix-2");
        assertThat(f2.isAliasOf(f3)).isFalse();
        assertThat(f3.isAliasOf(f2)).isFalse();
        assertThat(f1.hasAlias(f4)).isFalse();
        assertThat(f3.hasAlias(f1)).isFalse();
    }
}
