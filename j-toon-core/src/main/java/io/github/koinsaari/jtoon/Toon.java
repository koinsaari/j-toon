package io.github.koinsaari.jtoon;

public final class Toon {

    private Toon() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String encode(Object value) {
        return encode(value, ToonOptions.DEFAULT);
    }

    public static String encode(Object value, ToonOptions options) {
        return ToonEncoder.encode(value, options);
    }

    public static Object decode(String toon) {
        return decode(toon, ToonOptions.DEFAULT);
    }

    public static Object decode(String toon, ToonOptions options) {
        return ToonDecoder.decode(toon, options);
    }
}
