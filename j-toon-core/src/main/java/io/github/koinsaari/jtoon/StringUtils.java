package io.github.koinsaari.jtoon;

import java.util.regex.Pattern;

/**
 * String formatting and validation utilities for TOON encoding.
 * Handles quoting, escaping, and identifier validation.
 */
class StringUtils {

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_.]*$");
    private static final Pattern LOOKS_LIKE_BOOLEAN = Pattern.compile("^(true|false|null)$");
    private static final Pattern LOOKS_LIKE_NUMBER = Pattern.compile("^-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?$|^0\\d+$");
    private static final Pattern STRUCTURAL_TOKEN = Pattern.compile("^(\\[\\d+]|\\{.+}|\\[\\d+]:.+|- .+)");

    private StringUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Checks if a string is a valid TOON identifier.
     *
     * @param str string to check
     * @return true if valid identifier
     */
    static boolean isValidIdentifier(String str) {
        return IDENTIFIER_PATTERN.matcher(str).matches();
    }

    /**
     * Determines if a string value needs quotes in TOON format.
     *
     * @param str       string to check
     * @param delimiter current delimiter being used
     * @return true if quotes are required
     */
    static boolean needsQuotes(String str, String delimiter) {
        if (str.isEmpty()) return true;
        if (str.startsWith(" ") || str.endsWith(" ")) return true;
        if (str.contains(":")) return true;
        if (str.contains("\"")) return true;
        if (str.contains("\\")) return true;
        if (str.contains("\n") || str.contains("\r") || str.contains("\t")) return true;
        if (str.contains(delimiter)) return true;
        if (str.contains("[") || str.contains("]") || str.contains("{") || str.contains("}")) return true;
        if (LOOKS_LIKE_BOOLEAN.matcher(str).matches()) return true;
        if (LOOKS_LIKE_NUMBER.matcher(str).matches()) return true;
        if (STRUCTURAL_TOKEN.matcher(str).matches()) return true;
        return str.equals("-") || str.startsWith("-");
    }

    /**
     * Escapes special characters and adds quotes.
     *
     * @param str string to escape
     * @return quoted and escaped string
     */
    static String escapeAndQuote(String str) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : str.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    /**
     * Formats a string value for TOON output with context-aware quoting.
     *
     * @param str       string value to format
     * @param delimiter current delimiter being used
     * @return formatted string (quoted if necessary)
     */
    static String formatStringValue(String str, String delimiter) {
        if (needsQuotes(str, delimiter)) {
            return escapeAndQuote(str);
        }
        return str;
    }

    /**
     * Formats an object key for TOON output.
     *
     * @param key key to format
     * @return formatted key (quoted if not a valid identifier)
     */
    static String formatKey(String key) {
        if (isValidIdentifier(key)) {
            return key;
        }
        return escapeAndQuote(key);
    }
}
