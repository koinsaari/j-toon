package io.github.koinsaari.jtoon;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Encodes Java objects to TOON format.
 * <p>
 * Encoding strategy:
 * - Recursively traverses object graph via Jackson JsonNode representation
 * - Detects and optimizes array types: tabular (uniform objects), primitive (scalars), list (mixed)
 * - Applies smart quoting: only quotes when necessary (special chars, reserved keywords, etc.)
 * - Respects configuration: custom delimiters, indentation, length markers
 * - Handles type normalization: BigDecimal, Date, BigInt, NaN/Infinity conversions
 * <p>
 * Format generation:
 * - Primitives: "value" or "key: value"
 * - Objects: nested with indentation
 * - Tabular arrays: "items[2]{id,name}:\n  1,Ada\n  2,Bob"
 * - Primitive arrays: "tags[3]: a,b,c"
 * - List arrays: "items[2]:\n  - item1\n  - item2"
 */
class ToonEncoder {

    private ToonEncoder() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Encodes a Java object to TOON format string.
     * Normalizes object to Jackson JsonNode, then recursively encodes to TOON.
     *
     * @param value object to encode (Map, List, or any JSON-serializable type)
     * @param options encoding configuration (indent, delimiter, length markers)
     * @return TOON-formatted string with no trailing whitespace
     */
    static String encode(Object value, ToonOptions options) {
        JsonNode normalized = JsonNormalizer.normalize(value);
        LineWriter writer = new LineWriter(options.indent());
        encodeValue(normalized, writer, 0, options, null);
        return writer.getOutput();
    }

    /**
     * Recursively encodes a JsonNode value based on its type.
     * Dispatches to appropriate handler: null, boolean, number, string, array, or object.
     *
     * @param node value to encode
     * @param writer output builder with indentation tracking
     * @param depth current nesting level
     * @param options encoding configuration
     * @param key optional key name (null for root/array items)
     */
    private static void encodeValue(JsonNode node, LineWriter writer, int depth, ToonOptions options, String key) {
        if (node.isNull()) {
            encodeNull(writer, depth, key);
            return;
        }
        if (node.isBoolean()) {
            encodeBoolean(node, writer, depth, key);
            return;
        }
        if (node.isNumber()) {
            encodeNumber(node, writer, depth, key);
            return;
        }
        if (node.isString()) {
            encodeString(node, writer, depth, key, options);
            return;
        }
        if (node.isArray()) {
            encodeArray((ArrayNode) node, writer, depth, options, key);
            return;
        }
        if (node.isObject()) {
            encodeObject((ObjectNode) node, writer, depth, options, key);
        }
    }

    private static void encodeNull(LineWriter writer, int depth, String key) {
        writePrimitive(writer, depth, key, "null");
    }

    private static void encodeBoolean(JsonNode node, LineWriter writer, int depth, String key) {
        writePrimitive(writer, depth, key, String.valueOf(node.booleanValue()));
    }

    private static void encodeNumber(JsonNode node, LineWriter writer, int depth, String key) {
        writePrimitive(writer, depth, key, formatNumber(node));
    }

    private static void encodeString(JsonNode node, LineWriter writer, int depth, String key, ToonOptions options) {
        String formatted = StringUtils.formatStringValue(node.asString(), options.delimiter().getValue());
        writePrimitive(writer, depth, key, formatted);
    }

    private static void writePrimitive(LineWriter writer, int depth, String key, String value) {
        if (key != null) {
            writer.writeLine(depth, key + ": " + value);
            return;
        }
        writer.writeLine(depth, value);
    }

    /**
     * Formats number to TOON-safe string representation.
     * Handles integer, decimal, and special values (0, -0).
     * Avoids scientific notation by using stripTrailingZeros().toPlainString().
     *
     * @param node numeric JsonNode
     * @return string representation suitable for TOON
     */
    private static String formatNumber(JsonNode node) {
        if (node.isIntegralNumber()) {
            return String.valueOf(node.longValue());
        }
        BigDecimal decimal = node.decimalValue();
        if (decimal.compareTo(BigDecimal.ZERO) == 0) {
            return "0";
        }
        return decimal.stripTrailingZeros().toPlainString();
    }

    /**
     * Encodes an object as nested key-value pairs.
     * Empty objects output just "key:" with no fields.
     *
     * @param obj object to encode
     * @param writer output builder
     * @param depth current nesting level (incremented for nested fields)
     * @param options encoding configuration
     * @param key object's key name (null for root/array objects)
     */
    private static void encodeObject(ObjectNode obj, LineWriter writer, int depth, ToonOptions options, String key) {
        if (obj.isEmpty()) {
            if (key != null) {
                writer.writeLine(depth, key + ":");
            }
            return;
        }

        if (key != null) {
            writer.writeLine(depth, key + ":");
            depth++;
        }

        for (var entry : obj.properties()) {
            String fieldName = entry.getKey();
            String formattedKey = StringUtils.formatKey(fieldName);
            JsonNode fieldValue = entry.getValue();
            encodeValue(fieldValue, writer, depth, options, formattedKey);
        }
    }

    /**
     * Encodes an array with intelligent type detection and optimization.
     * Routes to: tabular (uniform objects), primitive (all scalars), or list (mixed/complex).
     * <p>
     * Detection logic:
     * - Tabular: all elements are objects with identical primitive-valued fields
     * - Primitive: all elements are scalar values (bool, number, string, null)
     * - List: everything else (mixed types, nested structures, non-uniform objects)
     *
     * @param array array to encode
     * @param writer output builder
     * @param depth current nesting level
     * @param options encoding configuration
     * @param key array's key name (null for root arrays)
     */
    private static void encodeArray(ArrayNode array, LineWriter writer, int depth, ToonOptions options, String key) {
        List<String> tabularKeys = detectTabularFormat(array);
        if (tabularKeys != null) {
            encodeTabularArray(array, tabularKeys, writer, depth, options, key);
            return;
        }

        if (isPrimitiveArray(array)) {
            encodePrimitiveArray(array, writer, depth, options, key);
            return;
        }

        encodeListArray(array, writer, depth, options, key);
    }

    private static void encodeListArray(ArrayNode array, LineWriter writer, int depth, ToonOptions options, String key) {
        int size = array.size();
        String lengthMarker = options.lengthMarker() ? "#" : "";
        String delimiter = options.delimiter().getValue();
        String header = formatArrayHeader(size, lengthMarker, delimiter);

        if (key != null) {
            writer.writeLine(depth, key + header + ":");
        } else {
            writer.writeLine(depth, header + ":");
        }

        for (JsonNode item : array) {
            if (item.isObject() && !item.isEmpty()) {
                encodeListItem((ObjectNode) item, writer, depth + 1, options);
            } else {
                writer.writeLine(depth + 1, "- " + formatInlineValue(item, options));
            }
        }
    }

    private static void encodePrimitiveArray(ArrayNode array, LineWriter writer, int depth, ToonOptions options, String key) {
        int size = array.size();
        String lengthMarker = options.lengthMarker() ? "#" : "";
        String delimiter = options.delimiter().getValue();

        List<String> values = new ArrayList<>();
        for (JsonNode item : array) {
            values.add(formatInlineValue(item, options));
        }

        String header = formatArrayHeader(size, lengthMarker, delimiter);
        String line = size == 0 ? header + ":" : header + ": " + String.join(delimiter, values);

        if (key != null) {
            writer.writeLine(depth, key + line);
        } else {
            writer.writeLine(depth, line);
        }
    }

    /**
     * Encodes tabular array: uniform objects with identical fields and primitive values.
     * Most token-efficient format for structured data arrays.
     * Example: users[2]{id,name,role}:\n  1,Alice,admin\n  2,Bob,user
     *
     * @param array array of objects
     * @param keys field names in consistent order
     * @param writer output builder
     * @param depth current nesting level
     * @param options encoding configuration
     * @param key array's key name
     */
    private static void encodeTabularArray(ArrayNode array, List<String> keys, LineWriter writer, int depth, ToonOptions options, String key) {
        int size = array.size();
        String lengthMarker = options.lengthMarker() ? "#" : "";
        String delimiter = options.delimiter().getValue();

        List<String> formattedKeys = keys.stream().map(StringUtils::formatKey).toList();
        String keysStr = String.join(delimiter, formattedKeys);
        String header = formatArrayHeader(size, lengthMarker, delimiter) + "{" + keysStr + "}";

        if (key != null) {
            writer.writeLine(depth, key + header + ":");
        } else {
            writer.writeLine(depth, header + ":");
        }

        for (JsonNode row : array) {
            ObjectNode obj = (ObjectNode) row;
            List<String> rowValues = new ArrayList<>();
            for (String k : keys) {
                JsonNode val = obj.get(k);
                rowValues.add(formatInlineValue(val, options));
            }
            String rowStr = String.join(delimiter, rowValues);
            writer.writeLine(depth + 1, rowStr);
        }
    }

    private static void encodeListItem(ObjectNode item, LineWriter writer, int depth, ToonOptions options) {
        boolean first = true;

        for (var entry : item.properties()) {
            String fieldName = entry.getKey();
            String formattedKey = StringUtils.formatKey(fieldName);
            JsonNode fieldValue = entry.getValue();

            if (first) {
                encodeFirstListItemField(fieldValue, formattedKey, writer, depth, options);
                first = false;
            } else {
                encodeValue(fieldValue, writer, depth + 1, options, formattedKey);
            }
        }
    }

    private static void encodeFirstListItemField(JsonNode fieldValue, String formattedKey, LineWriter writer, int depth, ToonOptions options) {
        if (fieldValue.isValueNode()) {
            String formatted = formatInlineValue(fieldValue, options);
            writer.writeLine(depth, "- " + formattedKey + ": " + formatted);
            return;
        }
        encodeValue(fieldValue, writer, depth, options, "- " + formattedKey);
    }

    private static String formatInlineValue(JsonNode node, ToonOptions options) {
        if (node.isNull()) {
            return "null";
        }
        if (node.isBoolean()) {
            return String.valueOf(node.booleanValue());
        }
        if (node.isNumber()) {
            return formatNumber(node);
        }
        if (node.isString()) {
            return StringUtils.formatStringValue(node.asString(), options.delimiter().getValue());
        }
        if (node.isArray()) {
            return formatInlineArray((ArrayNode) node, options);
        }
        return "null";
    }

    private static String formatInlineArray(ArrayNode array, ToonOptions options) {
        List<String> values = new ArrayList<>();
        for (JsonNode item : array) {
            values.add(formatInlineValue(item, options));
        }
        String lengthMarker = options.lengthMarker() ? "#" : "";
        String delimiter = options.delimiter().getValue();
        String header = formatArrayHeader(array.size(), lengthMarker, delimiter);
        return header + ": " + String.join(delimiter, values);
    }

    private static String formatArrayHeader(int size, String lengthMarker, String delimiter) {
        String delimiterDisplay = delimiter.equals(",") ? "" : delimiter;
        return "[" + lengthMarker + size + delimiterDisplay + "]";
    }

    private static boolean isPrimitiveArray(ArrayNode array) {
        for (JsonNode node : array) {
            if (!node.isValueNode()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Detects if array qualifies for tabular format.
     * Requirements:
     * 1. Non-empty array
     * 2. All elements are objects
     * 3. All objects have identical key sets (same order and names)
     * 4. All values in each object are primitives (not nested objects/arrays)
     * <p>
     * Critical: Uses objKeys.equals(keys) to enforce key order consistency.
     * LinkedHashMap preserves insertion order, enabling this check.
     *
     * @param array array to check
     * @return List of field names if tabular, null otherwise
     */
    private static List<String> detectTabularFormat(ArrayNode array) {
        if (array.isEmpty()) return null;

        JsonNode first = array.get(0);
        if (!first.isObject()) return null;

        ObjectNode firstObj = (ObjectNode) first;
        if (firstObj.isEmpty()) return null;

        List<String> keys = new ArrayList<>();
        for (var entry : firstObj.properties()) {
            keys.add(entry.getKey());
        }

        for (JsonNode node : array) {
            if (!node.isObject()) return null;

            ObjectNode obj = (ObjectNode) node;
            List<String> objKeys = new ArrayList<>();
            for (var entry : obj.properties()) {
                objKeys.add(entry.getKey());
            }

            if (!objKeys.equals(keys)) {
                return null;
            }

            for (String k : keys) {
                if (!obj.get(k).isValueNode()) {
                    return null;
                }
            }
        }

        return keys;
    }
}
