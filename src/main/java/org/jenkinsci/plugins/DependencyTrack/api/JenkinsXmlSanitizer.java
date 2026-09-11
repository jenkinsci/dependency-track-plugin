package org.jenkinsci.plugins.DependencyTrack.api;

import net.sf.json.JSONObject;

final class JenkinsXmlSanitizer {

    private JenkinsXmlSanitizer() {
    }

    static void sanitizeFinding(final JSONObject finding) {
        if (finding == null || !finding.containsKey("vulnerability")) {
            return;
        }

        final Object vulnerabilityValue = finding.get("vulnerability");
        if (!(vulnerabilityValue instanceof JSONObject vulnerability)) {
            return;
        }

        if (!vulnerability.containsKey("description")) {
            return;
        }

        final Object descriptionValue = vulnerability.get("description");
        if (descriptionValue instanceof String description) {
            vulnerability.element("description", sanitizeXml10(description));
        }
    }

    static String sanitizeXml10(final String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        final StringBuilder result = new StringBuilder(value.length());

        value.codePoints().forEach(codePoint -> {
            if (isValidXml10CodePoint(codePoint)) {
                result.appendCodePoint(codePoint);
            } else if (codePoint <= 0xFFFF) {
                result.append(String.format("\\u%04X", codePoint));
            } else {
                result.append(String.format("\\U%08X", codePoint));
            }
        });

        return result.toString();
    }

    private static boolean isValidXml10CodePoint(final int codePoint) {
        return codePoint == 0x09
                || codePoint == 0x0A
                || codePoint == 0x0D
                || (codePoint >= 0x20 && codePoint <= 0xD7FF)
                || (codePoint >= 0xE000 && codePoint <= 0xFFFD)
                || (codePoint >= 0x10000 && codePoint <= 0x10FFFF);
    }
}
