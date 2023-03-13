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
        Component c1 = Component.of("uuid-1", "name-1", "group-1", "version-1", "purl-1");
        Vulnerability v1 = new Vulnerability("uuid-1", "source-1", "vulnId-1", "title-1", "subtitle-1", "description-1", "recommendation-1", Severity.CRITICAL, 1, 2, "cweName-1");
        Analysis a1 = new Analysis("state-1", false);
        Finding f1 = new Finding(c1, v1, a1, "matrix-1");
        assertThat(FindingParser.parse(Files.contentOf(findings, StandardCharsets.UTF_8))).containsExactly(f1);
    }
}
