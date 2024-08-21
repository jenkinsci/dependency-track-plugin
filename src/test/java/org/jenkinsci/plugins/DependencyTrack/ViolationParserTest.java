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

import java.io.File;
import java.nio.charset.StandardCharsets;
import org.assertj.core.util.Files;
import org.jenkinsci.plugins.DependencyTrack.model.Component;
import org.jenkinsci.plugins.DependencyTrack.model.Violation;
import org.jenkinsci.plugins.DependencyTrack.model.ViolationState;
import org.jenkinsci.plugins.DependencyTrack.model.ViolationType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
public class ViolationParserTest {
    

    @Test
    void parseTest() {
        final var findings = Files.contentOf(new File("src/test/resources/violations.json"), StandardCharsets.UTF_8);
        final var c1 = new Component("uuid-1", "name-1", "group-1", "version-1", "purl-1");
        final var c2 = new Component("uuid-2", "name-2", "group-2", "version-2", "purl-2");
        final var v1 = new Violation("71e3ee99-86df-4819-a007-04dd9eb08278", ViolationType.SECURITY, ViolationState.INFO, "my-rule2", c2);
        final var v2 = new Violation("965e0b34-64cb-44ba-ae65-5c05b6f296be", ViolationType.SECURITY, ViolationState.WARN, "my-rule1", c1);
        final var v3 = new Violation("0f15c0b5-9849-41e8-9361-1a3f966bed76", ViolationType.SECURITY, ViolationState.FAIL, "my-rule3", c1);
        
        assertThat(ViolationParser.parse(findings)).usingRecursiveFieldByFieldElementComparator().containsExactly(v1, v2, v3);
    }
}
