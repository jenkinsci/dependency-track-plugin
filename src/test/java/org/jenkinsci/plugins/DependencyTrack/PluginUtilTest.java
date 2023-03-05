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

import hudson.util.FormValidation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
class PluginUtilTest {

    @Test
    void doCheckUrlTest() {
        assertThat(PluginUtil.doCheckUrl("")).isEqualTo(FormValidation.ok());
        assertThat(PluginUtil.doCheckUrl(null)).isEqualTo(FormValidation.ok());
        assertThat(PluginUtil.doCheckUrl("https://foo.bar/asd")).isEqualTo(FormValidation.ok());
        assertThat(PluginUtil.doCheckUrl("http://foo.bar/asd")).isEqualTo(FormValidation.ok());
        assertThat(PluginUtil.doCheckUrl("http://foo.bar")).isEqualTo(FormValidation.ok());

        assertThat(PluginUtil.doCheckUrl("foo")).hasMessage(Messages.Publisher_ConnectionTest_UrlMalformed()).hasFieldOrPropertyWithValue("kind", FormValidation.Kind.ERROR);
        assertThat(PluginUtil.doCheckUrl("ftp://foo.bar")).hasMessage(Messages.Publisher_ConnectionTest_InvalidProtocols()).hasFieldOrPropertyWithValue("kind", FormValidation.Kind.ERROR);
    }

    @Test
    void parseBaseUrlTest() {
        assertThat(PluginUtil.parseBaseUrl("  ")).isNull();
        assertThat(PluginUtil.parseBaseUrl("http://foo.bar")).isEqualTo("http://foo.bar");
        assertThat(PluginUtil.parseBaseUrl("http://foo.bar/")).isEqualTo("http://foo.bar");
        assertThat(PluginUtil.parseBaseUrl("http://foo.bar/asd/")).isEqualTo("http://foo.bar/asd");
        assertThat(PluginUtil.parseBaseUrl("http://foo.bar/asd/foo")).isEqualTo("http://foo.bar/asd/foo");
    }
}
