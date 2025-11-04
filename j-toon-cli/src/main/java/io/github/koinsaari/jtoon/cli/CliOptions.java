package io.github.koinsaari.jtoon.cli;

import io.github.koinsaari.jtoon.ToonOptions;

/**
 * CLI configuration options parsed from command-line arguments.
 */
public class CliOptions {
    public String input;
    public String output;
    public boolean encode;
    public boolean decode;
    public ToonOptions.Delimiter delimiter = ToonOptions.Delimiter.COMMA;
    public int indent = 2;
    public boolean lengthMarker = false;
    public boolean stats = false;
    public boolean strict = true;

    /**
     * Converts CLI options to TOON encoding/decoding options.
     */
    public ToonOptions toToonOptions() {
        return new ToonOptions(indent, delimiter, lengthMarker, strict);
    }
}
