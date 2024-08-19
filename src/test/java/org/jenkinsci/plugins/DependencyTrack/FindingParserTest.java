/*
 * Copyright 2020 OWASP.
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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.assertj.core.util.Files;
import org.jenkinsci.plugins.DependencyTrack.model.Analysis;
import org.jenkinsci.plugins.DependencyTrack.model.Component;
import org.jenkinsci.plugins.DependencyTrack.model.Finding;
import org.jenkinsci.plugins.DependencyTrack.model.Severity;
import org.jenkinsci.plugins.DependencyTrack.model.Vulnerability;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
class FindingParserTest {

    @Test
    void parseTest() {
        assertThat(FindingParser.parse("[]")).isEmpty();

        File findings = new File("src/test/resources/findings.json");

        Component c1 = new Component("uuid-1", "name-1", "group-1", "version-1", "purl-1");
        var aliases1 = List.of("GHSA-abcd-abcd-abcd", "FOO-12345");
        var v1 = new Vulnerability("uuid-1", "NVD", "vulnId-1", "title-1", "subtitle-1", "description-1", "recommendation-1", Severity.CRITICAL, 1, 2, "cweName-1", aliases1);
        Analysis a1 = new Analysis("state-1", false);
        Finding f1 = new Finding(c1, v1, a1, "matrix-1");

        var aliases2 = List.of("CVE-1234-123456");
        var c2 = new Component("uuid-2", "name-2", "group-2", "version-2", "purl-2");
        var v2 = new Vulnerability("uuid-2", "GITHUB", "GHSA-abcd-abcd-abcd", "title-1", "subtitle-1", "description-1", "recommendation-1", Severity.CRITICAL, 1, 2, "cweName-1", aliases2);
        var f2 = new Finding(c2, v2, a1, "matrix-3");

        var c3 = new Component("uuid-3", "name-3", "group-3", "version-3", "purl-3");
        var v3 = new Vulnerability("uuid-3", "FOO", "FOO-78945", "title-3", "subtitle-3", "description-3", "recommendation-3", Severity.CRITICAL, 1, null, null, null);
        var f3 = new Finding(c3, v3, a1, "matrix-4");

        assertThat(FindingParser.parse(Files.contentOf(findings, StandardCharsets.UTF_8))).usingRecursiveFieldByFieldElementComparator().containsExactly(f1, f2, f3);
    }
}
