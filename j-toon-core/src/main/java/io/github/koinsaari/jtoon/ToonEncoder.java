package io.github.koinsaari.jtoon;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Encodes Java objects to TOON format.
 * Handles primitives, objects, arrays, and tabular format detection.
 */
class ToonEncoder {

    private ToonEncoder() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Encodes a Java object to TOON format.
     *
     * @param value   object to encode
     * @param options encoding options
     * @return TOON-formatted string
     */
    static String encode(Object value, ToonOptions options) {
        JsonNode normalized = JsonNormalizer.normalize(value);
        LineWriter writer = new LineWriter(options.indent());
        encodeValue(normalized, writer, 0, options, null);
        return writer.getOutput();
    }

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

            if (objKeys.size() != keys.size() || !new HashSet<>(objKeys).containsAll(keys)) {
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
