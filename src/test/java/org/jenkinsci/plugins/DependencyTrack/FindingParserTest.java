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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Files;
import org.jenkinsci.plugins.DependencyTrack.model.Analysis;
import org.jenkinsci.plugins.DependencyTrack.model.Component;
import org.jenkinsci.plugins.DependencyTrack.model.Finding;
import org.jenkinsci.plugins.DependencyTrack.model.Severity;
import org.jenkinsci.plugins.DependencyTrack.model.Vulnerability;
import org.junit.Test;

/**
 *
 * @author Ronny "Sephiroth" Perinke <sephiroth@sephiroth-j.de>
 */
public class FindingParserTest {

    @Test
    public void parseTest() {
        assertThat(FindingParser.parse("[]")).isEmpty();

        File findings = new File("src/test/resources/findings.json");
        assertThat(FindingParser.parse(Files.contentOf(findings, StandardCharsets.UTF_8)))
        	.contains(createFinding(1, Severity.LOW, null, 2, null)).contains(createFinding(2, Severity.CRITICAL, "CVE-2022-34332", 3, "NVD"))
        	.contains(createFinding(3, Severity.CRITICAL, "CVE-2016-1000027", 4, "NVD")).size().isEqualTo(3);
    }

	private Finding createFinding(int n, Severity severity, String vid, int cweId, String source) {
		Component c = new Component(generate("uuid-1", n), generate("name-1", n), generate("group-1", n),
        		generate("version-1", n), generate("purl-1", n));
        Vulnerability v = new Vulnerability(generate("uuid-1", n), Optional.ofNullable(source).orElse(generate("source-1", n)),
        		Optional.ofNullable(vid).orElse(generate("vulnId-1", n)),
        		generate("title-1", n), generate("subtitle-1", n), generate("description-1", n),
        		generate("recommendation-1", n), severity, severity.ordinal(), cweId, generate("cweName-1", n));
        Analysis a = new Analysis(generate("state-1", n), false);
        Finding f = new Finding(c, v, a, generate("matrix-1", n));
		return f;
	}
    
    private static String generate(String input, int i) {
    	return StringUtils.removeEnd(input, "-1")+"-"+Integer.valueOf(i).toString().trim();
    }
}
