package io.github.koinsaari.jtoon;

/**
 * Utility for writing indented lines to build TOON output.
 * Handles indentation and line breaks automatically.
 */
class LineWriter {

    private final StringBuilder output = new StringBuilder();
    private final int indentSize;
    private boolean firstLine = true;

    /**
     * Creates a new line writer.
     *
     * @param indentSize number of spaces per indentation level
     */
    LineWriter(int indentSize) {
        this.indentSize = indentSize;
    }

    /**
     * Writes a line with the specified indentation depth.
     *
     * @param depth   indentation level
     * @param content line content to write
     */
    void writeLine(int depth, String content) {
        if (!firstLine) {
            output.append('\n');
        }
        firstLine = false;

        int spaces = depth * indentSize;
        output.append(" ".repeat(spaces));
        output.append(content);
    }

    /**
     * Returns the complete output string.
     *
     * @return formatted TOON output
     */
    String getOutput() {
        return output.toString();
    }
}
