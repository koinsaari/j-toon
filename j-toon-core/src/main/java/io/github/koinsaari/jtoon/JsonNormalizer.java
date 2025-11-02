package io.github.koinsaari.jtoon;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.NullNode;

/**
 * Normalizes Java objects to Jackson JsonNode representation.
 * Handles type conversion for dates, BigInteger, collections, etc.
 */
class JsonNormalizer {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonNormalizer() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Converts a Java object to JsonNode.
     *
     * @param value object to normalize
     * @return normalized JsonNode
     */
    static JsonNode normalize(Object value) {
        if (value == null) {
            return NullNode.getInstance();
        }

        try {
            return MAPPER.valueToTree(value);
        } catch (IllegalArgumentException e) {
            return NullNode.getInstance();
        }
    }
}
