package io.github.koinsaari.jtoon;

public record ToonOptions(
    int indent,
    Delimiter delimiter,
    boolean lengthMarker,
    boolean strict
) {

    public static final ToonOptions DEFAULT = new ToonOptions(2, Delimiter.COMMA, false, true);

    public ToonOptions() {
        this(2, Delimiter.COMMA, false, true);
    }

    public enum Delimiter {
        COMMA(","),
        TAB("\t"),
        PIPE("|");

        private final String value;

        Delimiter(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
