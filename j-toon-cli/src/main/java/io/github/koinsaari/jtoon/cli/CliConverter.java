package io.github.koinsaari.jtoon.cli;

import io.github.koinsaari.jtoon.Toon;
import tools.jackson.databind.ObjectMapper;

/**
 * Handles conversion logic between JSON and TOON formats.
 */
class CliConverter {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Converts between JSON and TOON based on detected format or explicit options.
     * Auto-detects if neither --encode nor --decode is specified.
     *
     * @param input   raw input string
     * @param options CLI options
     * @return converted output string
     */
    static String convert(String input, CliOptions options) throws Exception {
        boolean shouldEncode = options.encode;
        boolean shouldDecode = options.decode;

        if (!shouldEncode && !shouldDecode) {
            shouldEncode = detectFormat(options.input);
        }

        if (shouldEncode) {
            return encodeJsonToToon(input, options);
        } else {
            return decodeToonToJson(input, options);
        }
    }

    /**
     * Detects format based on input filename.
     *
     * @param input filename or null for stdin
     * @return true if should encode (JSON input), false if should decode (TOON input)
     */
    private static boolean detectFormat(String input) {
        if (input == null || input.equals("-")) {
            return true;
        }
        return input.endsWith(".json");
    }

    /**
     * Encodes JSON input to TOON format.
     */
    private static String encodeJsonToToon(String input, CliOptions options) throws Exception {
        Object jsonValue = MAPPER.readValue(input, Object.class);
        String toon = Toon.encode(jsonValue, options.toToonOptions());

        if (options.stats) {
            int jsonTokens = estimateJsonTokens(input);
            int toonTokens = estimateToonTokens(toon);
            int savings = (int) (100.0 * (1.0 - (double) toonTokens / jsonTokens));
            System.err.printf("JSON tokens: %d, TOON tokens: %d, savings: %d%%%n",
                jsonTokens, toonTokens, savings);
        }

        return toon;
    }

    /**
     * Decodes TOON input to JSON format.
     */
    private static String decodeToonToJson(String input, CliOptions options) throws Exception {
        Object toonValue = Toon.decode(input, options.toToonOptions());
        return MAPPER.writeValueAsString(toonValue);
    }

    /**
     * Rough estimate of token count for JSON (uses length heuristic).
     * Actual token counts vary by tokenizer.
     */
    private static int estimateJsonTokens(String json) {
        return (json.length() / 4) + 10;
    }

    /**
     * Rough estimate of token count for TOON (uses length heuristic).
     * Actual token counts vary by tokenizer.
     */
    private static int estimateToonTokens(String toon) {
        return (toon.length() / 4) + 10;
    }
}
