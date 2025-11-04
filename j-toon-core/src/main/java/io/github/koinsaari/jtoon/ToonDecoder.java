package io.github.koinsaari.jtoon;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decodes TOON-formatted strings back to Java objects.
 * <p>
 * Parsing strategy:
 * - Line-by-line processing with depth tracking (indentation-based)
 * - Distinguishes between arrays (keyed, tabular, list, primitive) and objects
 * - Handles quoted keys/values with special character escaping
 * - Supports multiple delimiter types (comma, tab, pipe)
 * <p>
 * Key patterns:
 * - Bare scalars: "true", "42", "hello"
 * - Key-value pairs: "name: Ada"
 * - Keyed arrays: "items[2]{id,name}: ..." or "items[2]{id,name}:\n  row1\n  row2"
 * - Tabular arrays: "items[2]{id,name}:\n  1,Ada\n  2,Bob"
 * - List arrays: "items[2]:\n  - item1\n  - item2"
 * - Nested objects: "user:\n  id: 123\n  name: Ada"
 */
class ToonDecoder {

    /**
     * Matches standalone array headers: [3], [#2], [3	], etc.
     */
    private static final Pattern ARRAY_HEADER_PATTERN = Pattern.compile("^\\[(#?)\\d+[\\t|]?]");

    /**
     * Matches tabular array headers with field names: [2]{id,name,role}:
     */
    private static final Pattern TABULAR_HEADER_PATTERN = Pattern.compile("^\\[(#?)\\d+[\\t|]?]\\{(.+)}:");

    /**
     * Matches keyed array headers: items[2]{id,name}: or tags[3]: or data[4]{id}:
     * Captures: group(1)=key, group(2)=#marker, group(3)=optional field spec
     */
    private static final Pattern KEYED_ARRAY_PATTERN = Pattern.compile("^(.+?)\\[(#?)\\d+[\\t|]?](\\{[^}]+})?:.*$");

    /**
     * Matches key-value pairs. Uses regex-based colon detection, but findUnquotedColon()
     * is preferred for handling quoted keys like "order:id": value
     */
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("^([^:]+):\\s?(.*)$");

    private ToonDecoder() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Decodes a TOON-formatted string to a Java object.
     *
     * @param toon    TOON-formatted input string
     * @param options parsing options (delimiter, indentation, strict mode)
     * @return parsed object (Map, List, scalar, or null)
     */
    static Object decode(String toon, ToonOptions options) {
        if (toon == null || toon.trim().isEmpty()) {
            return null;
        }

        String trimmed = toon.trim();
        Parser parser = new Parser(trimmed, options);
        return parser.parseValue();
    }

    /**
     * Inner parser class managing line-by-line parsing state.
     * Maintains currentLine index and uses recursive descent for nested structures.
     */
    private static class Parser {
        private final String[] lines;
        private final ToonOptions options;
        private final String delimiter;
        private int currentLine = 0;

        /**
         * Constructs parser with input string and options.
         *
         * @param toon    input to parse (split into lines)
         * @param options parsing configuration
         */
        Parser(String toon, ToonOptions options) {
            this.lines = toon.split("\n", -1);
            this.options = options;
            this.delimiter = options.delimiter().getValue();
        }

        /**
         * Parses the current line at root level (depth 0).
         * Routes to appropriate handler: arrays, keyed arrays, key-value pairs, or bare scalars.
         * Increments currentLine as it consumes input.
         *
         * @return parsed value (Map, List, scalar, or null)
         */
        Object parseValue() {
            if (currentLine >= lines.length) {
                return null;
            }

            String line = lines[currentLine];
            int depth = getDepth(line);

            if (depth > 0) {
                if (options.strict()) {
                    throw new IllegalArgumentException("Unexpected indentation at line " + currentLine);
                }
                return null;
            }

            String content = line.substring(depth * options.indent());

            if (content.startsWith("[")) {
                return parseArray(content, depth);
            }

            Matcher keyedArray = KEYED_ARRAY_PATTERN.matcher(content);
            if (keyedArray.matches()) {
                String key = unquoteString(keyedArray.group(1).trim());
                String arrayHeader = content.substring(keyedArray.group(1).length());

                List<Object> arrayValue = (List<Object>) parseArray(arrayHeader, depth);
                Map<String, Object> obj = new LinkedHashMap<>();
                obj.put(key, arrayValue);
                return obj;
            }

            int colonIdx = findUnquotedColon(content);
            if (colonIdx > 0) {
                String key = content.substring(0, colonIdx).trim();
                String value = content.substring(colonIdx + 1).trim();
                return parseKeyValuePair(key, value, depth, depth == 0);
            }

            currentLine++;
            return parseScalarValue(content);
        }

        /**
         * Parses array from header string and lines following it.
         * Detects array type (tabular, list, or primitive) and routes accordingly.
         * <p>
         * Tabular arrays have inline field spec: items[2]{id,name}:
         * Inline primitive arrays have values on same line: tags[3]: val1,val2,val3
         * Multiline primitive arrays have values on next line: data[2]:\n  val1,val2
         * List arrays start with "- " on next line: items[2]:\n  - item1\n  - item2
         *
         * @param header array header string (e.g. "[2]" or "items[2]{id,name}:")
         * @param depth  current indentation depth
         * @return parsed List of array elements
         */
        private Object parseArray(String header, int depth) {
            Matcher tabularMatcher = TABULAR_HEADER_PATTERN.matcher(header);
            Matcher arrayMatcher = ARRAY_HEADER_PATTERN.matcher(header);

            if (tabularMatcher.find()) {
                return parseTabularArray(header, depth);
            } else if (arrayMatcher.find()) {
                int headerEndIdx = arrayMatcher.end();
                String afterHeader = header.substring(headerEndIdx).trim();

                if (afterHeader.startsWith(":")) {
                    String inlineContent = afterHeader.substring(1).trim();

                    if (!inlineContent.isEmpty()) {
                        List<Object> result = parseArrayValues(inlineContent);
                        currentLine++;
                        return result;
                    }
                }

                currentLine++;
                if (currentLine < lines.length) {
                    String nextLine = lines[currentLine];
                    int nextDepth = getDepth(nextLine);
                    String nextContent = nextLine.substring(nextDepth * options.indent());

                    if (nextContent.startsWith("- ")) {
                        currentLine--;
                        return parseListArray(depth);
                    } else {
                        return parseArrayValues(nextContent);
                    }
                }
                return new ArrayList<>();
            }

            if (options.strict()) {
                throw new IllegalArgumentException("Invalid array header: " + header);
            }
            return null;
        }

        /**
         * Parses tabular array format where each row contains delimiter-separated values.
         * Example: items[2]{id,name}:\n  1,Ada\n  2,Bob
         *
         * @param header header string like "[2]{id,name}:"
         * @param depth  current indentation depth
         * @return List of Maps, one per row, with keys from header
         */
        private List<Object> parseTabularArray(String header, int depth) {
            Matcher matcher = TABULAR_HEADER_PATTERN.matcher(header);
            if (!matcher.find()) {
                return new ArrayList<>();
            }

            String keysStr = matcher.group(2);
            List<String> keys = parseTabularKeys(keysStr);

            List<Object> result = new ArrayList<>();
            currentLine++;

            while (currentLine < lines.length) {
                String line = lines[currentLine];
                int lineDepth = getDepth(line);

                if (lineDepth < depth + 1) {
                    break;
                }

                if (lineDepth == depth + 1) {
                    String rowContent = line.substring((depth + 1) * options.indent());
                    Map<String, Object> row = parseTabularRow(rowContent, keys);
                    result.add(row);
                }
                currentLine++;
            }

            return result;
        }

        /**
         * Parses list array format where items are prefixed with "- ".
         * Example: items[2]:\n  - item1\n  - item2
         * Items can be scalars or objects with nested fields.
         *
         * @param depth current indentation depth
         * @return List of parsed items (scalars or Maps)
         */
        private List<Object> parseListArray(int depth) {
            List<Object> result = new ArrayList<>();
            currentLine++;

            while (currentLine < lines.length) {
                String line = lines[currentLine];
                int lineDepth = getDepth(line);

                if (lineDepth < depth + 1) {
                    break;
                }

                if (lineDepth == depth + 1) {
                    String content = line.substring((depth + 1) * options.indent());
                    if (content.startsWith("- ")) {
                        result.add(parseListItem(content, depth));
                        continue;
                    }
                }
                currentLine++;
            }

            return result;
        }

        /**
         * Parses a single list item starting with "- ".
         * Item can be a scalar value (e.g. "- text") or an object (e.g. "- id: 1" with nested fields).
         *
         * @param content line content after indentation (starting with "- ")
         * @param depth   current indentation depth
         * @return parsed item (scalar or Map)
         */
        private Object parseListItem(String content, int depth) {
            String itemContent = content.substring(2).trim();
            Matcher keyValue = KEY_VALUE_PATTERN.matcher(itemContent);

            if (!keyValue.matches()) {
                currentLine++;
                return parseScalarValue(itemContent);
            }

            String key = unquoteString(keyValue.group(1).trim());
            String value = keyValue.group(2).trim();

            Map<String, Object> item = new LinkedHashMap<>();
            item.put(key, parseScalarValue(value));

            currentLine++;
            parseListItemFields(item, depth);

            return item;
        }

        private void parseListItemFields(Map<String, Object> item, int depth) {
            while (currentLine < lines.length) {
                String line = lines[currentLine];
                int lineDepth = getDepth(line);

                if (lineDepth < depth + 2) {
                    break;
                }

                if (lineDepth == depth + 2) {
                    String fieldContent = line.substring((depth + 2) * options.indent());
                    Matcher fieldMatcher = KEY_VALUE_PATTERN.matcher(fieldContent);

                    if (fieldMatcher.matches()) {
                        String fieldKey = unquoteString(fieldMatcher.group(1).trim());
                        String fieldValue = fieldMatcher.group(2).trim();
                        item.put(fieldKey, parseScalarValue(fieldValue));
                    }
                }
                currentLine++;
            }
        }

        private Map<String, Object> parseTabularRow(String rowContent, List<String> keys) {
            Map<String, Object> row = new LinkedHashMap<>();
            List<Object> values = parseArrayValues(rowContent);

            for (int i = 0; i < keys.size() && i < values.size(); i++) {
                row.put(keys.get(i), values.get(i));
            }

            return row;
        }

        private List<String> parseTabularKeys(String keysStr) {
            List<String> result = new ArrayList<>();
            List<String> rawValues = parseDelimitedValues(keysStr);
            for (String key : rawValues) {
                result.add(unquoteString(key));
            }
            return result;
        }

        private List<Object> parseArrayValues(String values) {
            List<Object> result = new ArrayList<>();
            List<String> rawValues = parseDelimitedValues(values);
            for (String value : rawValues) {
                result.add(parseScalarValue(value));
            }
            return result;
        }

        private List<String> parseDelimitedValues(String input) {
            List<String> result = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            boolean inQuotes = false;
            boolean escaped = false;

            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);

                if (escaped) {
                    current.append(unescapeCharacter(c));
                    escaped = false;
                    continue;
                }

                if (c == '\\') {
                    escaped = true;
                    continue;
                }

                if (c == '"') {
                    inQuotes = !inQuotes;
                    continue;
                }

                if (c == delimiter.charAt(0) && !inQuotes) {
                    result.add(current.toString().trim());
                    current = new StringBuilder();
                    continue;
                }

                current.append(c);
            }

            if (!current.isEmpty() || input.endsWith(String.valueOf(delimiter))) {
                result.add(current.toString().trim());
            }

            return result;
        }

        /**
         * Parses additional key-value pairs and keyed arrays at root level (depth 0).
         * Called after parsing the first field to collect all remaining root-level fields.
         * Example: first "id" line triggers this to parse "name" and "active" lines.
         *
         * @param obj   map to populate with additional fields
         * @param depth current depth (should be 0 for root level)
         */
        private void parseRootObjectFields(Map<String, Object> obj, int depth) {
            while (currentLine < lines.length) {
                String line = lines[currentLine];
                int lineDepth = getDepth(line);

                if (lineDepth != depth) {
                    break;
                }

                String content = line.substring(depth * options.indent());

                Matcher keyedArray = KEYED_ARRAY_PATTERN.matcher(content);
                if (keyedArray.matches()) {
                    String key = unquoteString(keyedArray.group(1).trim());
                    String arrayHeader = content.substring(keyedArray.group(1).length());
                    List<Object> arrayValue = (List<Object>) parseArray(arrayHeader, depth);
                    obj.put(key, arrayValue);
                    continue;
                }

                int colonIdx = findUnquotedColon(content);
                if (colonIdx <= 0) {
                    break;
                }

                String key = content.substring(0, colonIdx).trim();
                String value = content.substring(colonIdx + 1).trim();
                parseKeyValuePairIntoMap(obj, key, value, depth);
                currentLine++;
            }
        }

        private Object parseNestedObject(int parentDepth) {
            Map<String, Object> obj = new LinkedHashMap<>();

            while (currentLine < lines.length) {
                String line = lines[currentLine];
                int depth = getDepth(line);

                if (depth <= parentDepth) {
                    break;
                }

                if (depth == parentDepth + 1) {
                    String content = line.substring((parentDepth + 1) * options.indent());

                    Matcher keyedArray = KEYED_ARRAY_PATTERN.matcher(content);
                    if (keyedArray.matches()) {
                        String key = unquoteString(keyedArray.group(1).trim());
                        String arrayHeader = content.substring(keyedArray.group(1).length());
                        List<Object> arrayValue = (List<Object>) parseArray(arrayHeader, parentDepth + 1);
                        obj.put(key, arrayValue);
                        continue;
                    }

                    int colonIdx = findUnquotedColon(content);
                    if (colonIdx > 0) {
                        String key = content.substring(0, colonIdx).trim();
                        String value = content.substring(colonIdx + 1).trim();
                        parseKeyValuePairIntoMap(obj, key, value, depth);
                    }
                }
                currentLine++;
            }

            return obj;
        }

        /**
         * Parses a scalar value string into appropriate Java type.
         * Handles null, booleans, numbers (Long/Double), and strings.
         * Empty strings are returned as empty string literal.
         *
         * @param value string value to parse
         * @return Boolean, Long, Double, String, or null
         */
        private Object parseScalarValue(String value) {
            if (value == null || value.isEmpty()) {
                return "";
            }

            switch (value) {
                case "null" -> {
                    return null;
                }

                case "true" -> {
                    return true;
                }
                case "false" -> {
                    return false;
                }
            }

            if (value.startsWith("\"") && value.endsWith("\"")) {
                return unquoteString(value);
            }

            try {
                if (value.contains(".")) {
                    return Double.parseDouble(value);
                } else {
                    return Long.parseLong(value);
                }
            } catch (NumberFormatException e) {
                return value;
            }
        }

        private String unquoteString(String str) {
            if (str == null || str.length() < 2) {
                return str;
            }

            if (str.startsWith("\"") && str.endsWith("\"")) {
                String unquoted = str.substring(1, str.length() - 1);
                StringBuilder result = new StringBuilder();
                boolean escaped = false;

                for (char c : unquoted.toCharArray()) {
                    if (escaped) {
                        result.append(unescapeCharacter(c));
                        escaped = false;
                    } else if (c == '\\') {
                        escaped = true;
                    } else {
                        result.append(c);
                    }
                }

                return result.toString();
            }

            return str;
        }

        /**
         * Parses a key-value pair at root level, creating and returning a new Map.
         * Handles both scalar values and nested objects.
         * Optionally calls parseRootObjectFields to collect additional root fields.
         *
         * @param key             unquoted key string
         * @param value           value string (may be empty for nested objects)
         * @param depth           current depth
         * @param parseRootFields if true, collect additional fields at this depth
         * @return Map containing this key and any additional collected fields
         */
        private Object parseKeyValuePair(String key, String value, int depth, boolean parseRootFields) {
            key = unquoteString(key);

            if (currentLine + 1 < lines.length) {
                int nextDepth = getDepth(lines[currentLine + 1]);
                if (nextDepth > depth) {
                    currentLine++;
                    Map<String, Object> obj = new LinkedHashMap<>();
                    obj.put(key, parseNestedObject(depth));

                    if (parseRootFields) {
                        parseRootObjectFields(obj, depth);
                    }
                    return obj;
                }
            }

            currentLine++;
            Object parsedValue = parseScalarValue(value);
            Map<String, Object> obj = new LinkedHashMap<>();
            obj.put(key, parsedValue);

            if (parseRootFields) {
                parseRootObjectFields(obj, depth);
            }
            return obj;
        }

        /**
         * Parses a key-value pair and adds it to an existing map.
         * Handles both scalar values and nested objects with lookahead for deeper indentation.
         *
         * @param map   existing map to populate
         * @param key   unquoted key string
         * @param value value string (may be empty for nested objects)
         * @param depth current depth for nested object detection
         */
        private void parseKeyValuePairIntoMap(Map<String, Object> map, String key, String value, int depth) {
            key = unquoteString(key);

            if (currentLine + 1 < lines.length) {
                int nextDepth = getDepth(lines[currentLine + 1]);
                if (nextDepth > depth) {
                    currentLine++;
                    map.put(key, parseNestedObject(depth));
                    return;
                }
            }

            map.put(key, parseScalarValue(value));
        }

        /**
         * Finds the index of the first unquoted colon in a key-value line.
         * Critical for handling quoted keys like "order:id": value or "full name": Ada.
         * Treats colons inside quotes as literal characters, not key-value separators.
         * <p>
         * Example:
         * "order:id": 7  -> returns index after closing quote of key
         * name: Ada      -> returns index of the unquoted colon
         * "a:b": "c:d"   -> returns index of unquoted colon between key and value
         *
         * @param content line content to search
         * @return index of first unquoted colon, or -1 if none found
         */
        private int findUnquotedColon(String content) {
            boolean inQuotes = false;
            boolean escaped = false;

            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);

                if (escaped) {
                    escaped = false;
                    continue;
                }

                if (c == '\\') {
                    escaped = true;
                    continue;
                }

                if (c == '"') {
                    inQuotes = !inQuotes;
                    continue;
                }

                if (c == ':' && !inQuotes) {
                    return i;
                }
            }

            return -1;
        }

        private char unescapeCharacter(char c) {
            return switch (c) {
                case 'n' -> '\n';
                case 'r' -> '\r';
                case 't' -> '\t';
                case '"' -> '"';
                case '\\' -> '\\';
                default -> c;
            };
        }

        /**
         * Calculates indentation depth (nesting level) of a line.
         * Counts leading spaces in multiples of the configured indent size.
         * Example: with indentSize=2, "    key: value" has depth 2.
         *
         * @param line line to measure
         * @return depth (number of indent levels)
         */
        private int getDepth(String line) {
            int depth = 0;
            int indentSize = options.indent();

            for (int i = 0; i < line.length(); i += indentSize) {
                if (i + indentSize <= line.length() && line.substring(i, i + indentSize).equals(" ".repeat(indentSize))) {
                    depth++;
                } else {
                    break;
                }
            }

            return depth;
        }
    }
}
