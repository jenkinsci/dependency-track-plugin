package org.jenkinsci.plugins.DependencyTrack.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JenkinsXmlSanitizerTest {

    @Test
    void shouldEscapeUffff() {
        final String uffff = new String(Character.toChars(0xFFFF));

        final String value =
                "if (/[\\x80-" + uffff + "]/.test(user)) {";

        final String sanitized =
                JenkinsXmlSanitizer.sanitizeXml11(value);

        assertThat(sanitized)
                .isEqualTo("if (/[\\x80-\\uFFFF]/.test(user)) {");
    }

    @Test
    void shouldPreserveValidUnicode() {
        final String value = "ASCII é € 日本語 😀\t\r\n";

        assertThat(JenkinsXmlSanitizer.sanitizeXml11(value))
                .isEqualTo(value);
    }

    @Test
    void shouldEscapeOtherInvalidXml11Characters() {
        final String value =
                "a"
                        + (char) 0x0000
                        + "b"
                        + new String(Character.toChars(0xFFFE))
                        + "c"
                        + new String(Character.toChars(0xFFFF))
                        + "d";

        assertThat(JenkinsXmlSanitizer.sanitizeXml11(value))
                .isEqualTo("a\\u0000b\\uFFFEc\\uFFFFd");
    }

    @Test
    void shouldPreserveXml11ControlCharacters() {
        final String value = "a" + (char) 0x000B + "b";

        assertThat(JenkinsXmlSanitizer.sanitizeXml11(value))
                .isEqualTo(value);
    }

    @Test
    void shouldHandleNullAndEmptyStrings() {
        assertThat(JenkinsXmlSanitizer.sanitizeXml11(null))
                .isNull();

        assertThat(JenkinsXmlSanitizer.sanitizeXml11(""))
                .isEmpty();
    }
}
