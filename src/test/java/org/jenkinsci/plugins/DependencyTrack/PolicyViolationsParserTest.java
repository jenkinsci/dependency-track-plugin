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

import org.assertj.core.util.Files;
import org.jenkinsci.plugins.DependencyTrack.model.*;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyViolationsParserTest
{
    @Test
    public void parseTest()
    {
        assertThat(PolicyViolationsParser.parse("[]")).isEmpty();

        final File policyViolations = new File("src/test/resources/policyViolations.json");

        final Component component = Component.of("uuid-1", "name-1", "group-1", "version-1", "purl-1");

        final PolicyCondition policyCondition =
                PolicyCondition
                        .of(
                                "uuid-1",
                                Policy.of("uuid-1", "name-1", ViolationState.FAIL));

        final PolicyViolation policyViolation =
                PolicyViolation
                        .of(
                                "uuid-1",
                                Type.LICENSE,
                                component,
                                policyCondition);

        assertThat(PolicyViolationsParser.parse(Files.contentOf(policyViolations, StandardCharsets.UTF_8))).containsExactly(policyViolation);
    }
}
