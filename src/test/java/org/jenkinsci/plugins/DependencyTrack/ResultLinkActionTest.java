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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
class ResultLinkActionTest {

    @ParameterizedTest
    @CsvSource({
        ",",
        "'',''",
        ",'an-id'",
        "'http://foo.bar',",})
    void testDisableSituations(String url, String projectId) {
        ResultLinkAction uut = new ResultLinkAction(url, projectId);
        assertThat(uut.getUrlName()).isNull();
        assertThat(uut.getDisplayName()).isNull();
        assertThat(uut.getIconFileName()).isNull();
    }

    @Test
    void testWithUrlAndProjectId() {
        ResultLinkAction uut = new ResultLinkAction("http://foo.bar", "an-id\"");
        assertThat(uut.getUrlName()).isEqualTo("http://foo.bar/projects/an-id%22");
        assertThat(uut.getDisplayName()).isNotEmpty();
        assertThat(uut.getIconFileName()).isNotEmpty();
    }

}
