package org.jenkinsci.plugins.DependencyTrack.model;

final class JenkinsXmlSanitizer {

    private JenkinsXmlSanitizer() {
    }

    static String sanitizeXml11(final String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        final StringBuilder result = new StringBuilder(value.length());

        value.codePoints().forEach(codePoint -> {
            if (isValidXml11CodePoint(codePoint)) {
                result.appendCodePoint(codePoint);
            } else if (codePoint <= 0xFFFF) {
                result.append(String.format("\\u%04X", codePoint));
            } else {
                result.append(String.format("\\U%08X", codePoint));
            }
        });

        return result.toString();
    }

    private static boolean isValidXml11CodePoint(final int codePoint) {
        return (codePoint >= 0x01 && codePoint <= 0xD7FF)
                || (codePoint >= 0xE000 && codePoint <= 0xFFFD)
                || (codePoint >= 0x10000 && codePoint <= 0x10FFFF);
    }
}
