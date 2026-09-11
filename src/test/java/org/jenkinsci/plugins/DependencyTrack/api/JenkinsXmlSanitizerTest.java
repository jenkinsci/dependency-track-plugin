package org.jenkinsci.plugins.DependencyTrack.api;

import net.sf.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JenkinsXmlSanitizerTest {

    @Test
    void shouldEscapeUffff() {
        final String uffff = new String(Character.toChars(0xFFFF));

        final String value =
                "if (/[\\x80-" + uffff + "]/.test(user)) {";

        final String sanitized =
                JenkinsXmlSanitizer.sanitizeXml10(value);

        assertThat(sanitized)
                .isEqualTo("if (/[\\x80-\\uFFFF]/.test(user)) {");
    }

    @Test
    void shouldPreserveValidUnicode() {
        final String value = "ASCII é € 日本語 😀\t\r\n";

        assertThat(JenkinsXmlSanitizer.sanitizeXml10(value))
                .isEqualTo(value);
    }

    @Test
    void shouldEscapeOtherInvalidXml10Characters() {
        final String value =
                "a"
                        + (char) 0x000B
                        + "b"
                        + new String(Character.toChars(0xFFFE))
                        + "c"
                        + new String(Character.toChars(0xFFFF))
                        + "d";

        assertThat(JenkinsXmlSanitizer.sanitizeXml10(value))
                .isEqualTo("a\\u000Bb\\uFFFEc\\uFFFFd");
    }

    @Test
    void shouldSanitizeVulnerabilityDescriptionInFinding() {
        final String uffff =
                new String(Character.toChars(0xFFFF));

        final JSONObject vulnerability =
                new JSONObject()
                        .element("vulnId", "GHSA-wmmp-3585-3rmp")
                        .element(
                                "description",
                                "if (/[\\x80-" + uffff + "]/.test(user)) {");

        final JSONObject finding =
                new JSONObject()
                        .element("vulnerability", vulnerability);

        JenkinsXmlSanitizer.sanitizeFinding(finding);

        assertThat(
                finding
                        .getJSONObject("vulnerability")
                        .getString("description"))
                .isEqualTo("if (/[\\x80-\\uFFFF]/.test(user)) {");
    }

    @Test
    void shouldIgnoreFindingWithoutDescription() {
        final JSONObject finding =
                new JSONObject()
                        .element(
                                "vulnerability",
                                new JSONObject()
                                        .element("vulnId", "GHSA-test"));

        JenkinsXmlSanitizer.sanitizeFinding(finding);

        assertThat(
                finding.getJSONObject("vulnerability")
                        .containsKey("description"))
                .isFalse();
    }

    @Test
    void shouldHandleNullAndEmptyStrings() {
        assertThat(JenkinsXmlSanitizer.sanitizeXml10(null))
                .isNull();

        assertThat(JenkinsXmlSanitizer.sanitizeXml10(""))
                .isEmpty();
    }
}
