package io.github.koinsaari.jtoon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.github.koinsaari.jtoon.ToonOptions.Delimiter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SuppressWarnings("unchecked")
class ToonDecoderTest {

    private static Object decode(String toon) {
        return Toon.decode(toon);
    }

    private static Object decode(String toon, ToonOptions options) {
        return Toon.decode(toon, options);
    }

    @Nested
    @DisplayName("Primitives")
    class Primitives {

        @Test
        @DisplayName("decodes null")
        void decodesNull() {
            assertNull(decode("null"));
        }

        @Test
        @DisplayName("decodes booleans")
        void decodesBooleans() {
            assertEquals(true, decode("true"));
            assertEquals(false, decode("false"));
        }

        @Test
        @DisplayName("decodes integers")
        void decodesIntegers() {
            assertEquals(0L, decode("0"));
            assertEquals(42L, decode("42"));
            assertEquals(-7L, decode("-7"));
        }

        @Test
        @DisplayName("decodes floating point numbers")
        void decodesFloatingPoint() {
            assertEquals(3.14, (double) decode("3.14"), 0.01);
            assertEquals(-0.5, (double) decode("-0.5"), 0.01);
        }

        @Test
        @DisplayName("decodes safe strings")
        void decodesSafeStrings() {
            assertEquals("hello", decode("hello"));
            assertEquals("Ada_99", decode("Ada_99"));
        }

        @Test
        @DisplayName("decodes quoted strings")
        void decodesQuotedStrings() {
            assertEquals("", decode("\"\""));
            assertEquals("hello world", decode("\"hello world\""));
        }

        @Test
        @DisplayName("decodes strings with special characters")
        void decodesSpecialStrings() {
            assertEquals("true", decode("\"true\""));
            assertEquals("42", decode("\"42\""));
            assertEquals("a:b", decode("\"a:b\""));
        }

        @Test
        @DisplayName("decodes escaped characters")
        void decodesEscapedCharacters() {
            assertEquals("line1\nline2", decode("\"line1\\nline2\""));
            assertEquals("tab\there", decode("\"tab\\there\""));
            assertEquals("quote\"here", decode("\"quote\\\"here\""));
        }
    }

    @Nested
    @DisplayName("Objects")
    class Objects {

        @Test
        @DisplayName("decodes simple object")
        void decodesSimpleObject() {
            Map<String, Object> result = (Map<String, Object>) decode("id: 123\nname: Ada\nactive: true");

            assertEquals(123L, result.get("id"));
            assertEquals("Ada", result.get("name"));
            assertEquals(true, result.get("active"));
        }

        @Test
        @DisplayName("decodes nested objects")
        void decodesNestedObjects() {
            String toon = "name: Alice\naddress:\n  city: Springfield\n  zip: \"12345\"";
            Map<String, Object> result = (Map<String, Object>) decode(toon);

            assertEquals("Alice", result.get("name"));

            Map<String, Object> address = (Map<String, Object>) result.get("address");
            assertEquals("Springfield", address.get("city"));
            assertEquals("12345", address.get("zip"));
        }

        @Test
        @DisplayName("decodes deeply nested objects")
        void decodesDeeplyNested() {
            String toon = "a:\n  b:\n    c: deep";
            Map<String, Object> result = (Map<String, Object>) decode(toon);

            Map<String, Object> a = (Map<String, Object>) result.get("a");
            Map<String, Object> b = (Map<String, Object>) a.get("b");
            assertEquals("deep", b.get("c"));
        }

        @Test
        @DisplayName("decodes keys with special characters")
        void decodesSpecialKeys() {
            String toon = "\"order:id\": 7\n\"full name\": Ada";
            Map<String, Object> result = (Map<String, Object>) decode(toon);

            assertEquals(7L, result.get("order:id"));
            assertEquals("Ada", result.get("full name"));
        }
    }

    @Nested
    @DisplayName("Primitive Arrays")
    class PrimitiveArrays {

        @Test
        @DisplayName("decodes inline primitive array")
        void decodesInlineArray() {
            String toon = "tags[3]: reading,gaming,coding";
            Map<String, Object> result = (Map<String, Object>) decode(toon);

            List<Object> tags = (List<Object>) result.get("tags");
            assertEquals(3, tags.size());
            assertEquals("reading", tags.get(0));
            assertEquals("gaming", tags.get(1));
            assertEquals("coding", tags.get(2));
        }

        @Test
        @DisplayName("decodes empty array")
        void decodesEmptyArray() {
            String toon = "items[0]:";
            Map<String, Object> result = (Map<String, Object>) decode(toon);

            List<Object> items = (List<Object>) result.get("items");
            assertEquals(0, items.size());
        }

        @Test
        @DisplayName("decodes mixed primitive types")
        void decodesMixedPrimitives() {
            String toon = "data[4]: text,42,true,null";
            Map<String, Object> result = (Map<String, Object>) decode(toon);

            List<Object> data = (List<Object>) result.get("data");
            assertEquals(4, data.size());
            assertEquals("text", data.get(0));
            assertEquals(42L, data.get(1));
            assertEquals(true, data.get(2));
            assertNull(data.get(3));
        }

        @Test
        @DisplayName("decodes array with quoted strings")
        void decodesQuotedStrings() {
            String toon = "items[3]: a,\"b,c\",\"d:e\"";
            Map<String, Object> result = (Map<String, Object>) decode(toon);

            List<Object> items = (List<Object>) result.get("items");
            assertEquals("a", items.get(0));
            assertEquals("b,c", items.get(1));
            assertEquals("d:e", items.get(2));
        }
    }

    @Nested
    @DisplayName("Tabular Arrays")
    class TabularArrays {

        @Test
        @DisplayName("decodes tabular format")
        void decodesTabularFormat() {
            String toon = "users[2]{id,name,role}:\n  1,Alice,admin\n  2,Bob,user";
            Map<String, Object> result = (Map<String, Object>) decode(toon);

            List<Object> users = (List<Object>) result.get("users");
            assertEquals(2, users.size());

            Map<String, Object> user1 = (Map<String, Object>) users.get(0);
            assertEquals(1L, user1.get("id"));
            assertEquals("Alice", user1.get("name"));
            assertEquals("admin", user1.get("role"));

            Map<String, Object> user2 = (Map<String, Object>) users.get(1);
            assertEquals(2L, user2.get("id"));
            assertEquals("Bob", user2.get("name"));
            assertEquals("user", user2.get("role"));
        }

        @Test
        @DisplayName("handles null values in tabular format")
        void handlesNullInTabular() {
            String toon = "items[2]{id,value}:\n  1,null\n  2,test";
            Map<String, Object> result = (Map<String, Object>) decode(toon);

            List<Object> items = (List<Object>) result.get("items");
            Map<String, Object> item1 = (Map<String, Object>) items.get(0);
            assertNull(item1.get("value"));

            Map<String, Object> item2 = (Map<String, Object>) items.get(1);
            assertEquals("test", item2.get("value"));
        }

        @Test
        @DisplayName("decodes quoted values in tabular rows")
        void decodesQuotedTabularValues() {
            String toon = "items[2]{sku,desc}:\n  \"A,1\",cool\n  B2,\"test:value\"";
            Map<String, Object> result = (Map<String, Object>) decode(toon);

            List<Object> items = (List<Object>) result.get("items");
            Map<String, Object> item1 = (Map<String, Object>) items.get(0);
            assertEquals("A,1", item1.get("sku"));
            assertEquals("cool", item1.get("desc"));

            Map<String, Object> item2 = (Map<String, Object>) items.get(1);
            assertEquals("B2", item2.get("sku"));
            assertEquals("test:value", item2.get("desc"));
        }

        @Test
        @DisplayName("decodes single column tabular format")
        void decodesSingleColumn() {
            String toon = "items[2]{id}:\n  1\n  2";
            Map<String, Object> result = (Map<String, Object>) decode(toon);

            List<Object> items = (List<Object>) result.get("items");
            Map<String, Object> item1 = (Map<String, Object>) items.get(0);
            assertEquals(1L, item1.get("id"));

            Map<String, Object> item2 = (Map<String, Object>) items.get(1);
            assertEquals(2L, item2.get("id"));
        }
    }

    @Nested
    @DisplayName("Delimiter Options")
    class DelimiterOptions {

        @Test
        @DisplayName("decodes with tab delimiter")
        void decodesWithTab() {
            String toon = "tags[3\t]: a\tb\tc";
            ToonOptions options = new ToonOptions(2, Delimiter.TAB, false, true);

            Map<String, Object> result = (Map<String, Object>) decode(toon, options);
            List<Object> tags = (List<Object>) result.get("tags");

            assertEquals(3, tags.size());
            assertEquals("a", tags.get(0));
            assertEquals("b", tags.get(1));
            assertEquals("c", tags.get(2));
        }

        @Test
        @DisplayName("decodes with pipe delimiter")
        void decodesWithPipe() {
            String toon = "tags[3|]: a|b|c";
            ToonOptions options = new ToonOptions(2, Delimiter.PIPE, false, true);

            Map<String, Object> result = (Map<String, Object>) decode(toon, options);
            List<Object> tags = (List<Object>) result.get("tags");

            assertEquals(3, tags.size());
            assertEquals("a", tags.get(0));
            assertEquals("b", tags.get(1));
            assertEquals("c", tags.get(2));
        }

        @Test
        @DisplayName("decodes tabular with tab delimiter")
        void decodesTabularWithTab() {
            String toon = "users[2\t]{id\tname}:\n  1\tAlice\n  2\tBob";
            ToonOptions options = new ToonOptions(2, Delimiter.TAB, false, true);

            Map<String, Object> result = (Map<String, Object>) decode(toon, options);
            List<Object> users = (List<Object>) result.get("users");

            assertEquals(2, users.size());
            Map<String, Object> user1 = (Map<String, Object>) users.get(0);
            assertEquals(1L, user1.get("id"));
            assertEquals("Alice", user1.get("name"));
        }
    }

    @Nested
    @DisplayName("Length Marker Option")
    class LengthMarkerOption {

        @Test
        @DisplayName("decodes with length marker")
        void decodesWithLengthMarker() {
            String toon = "tags[#3]: a,b,c";
            ToonOptions options = new ToonOptions(2, Delimiter.COMMA, true, true);

            Map<String, Object> result = (Map<String, Object>) decode(toon, options);
            List<Object> tags = (List<Object>) result.get("tags");

            assertEquals(3, tags.size());
            assertEquals("a", tags.get(0));
            assertEquals("b", tags.get(1));
            assertEquals("c", tags.get(2));
        }

        @Test
        @DisplayName("decodes tabular with length marker")
        void decodesTabularWithLengthMarker() {
            String toon = "items[#2]{id}:\n  1\n  2";
            ToonOptions options = new ToonOptions(2, Delimiter.COMMA, true, true);

            Map<String, Object> result = (Map<String, Object>) decode(toon, options);
            List<Object> items = (List<Object>) result.get("items");

            assertEquals(2, items.size());
        }
    }

    @Nested
    @DisplayName("Complex Structures")
    class ComplexStructures {

        @Test
        @DisplayName("decodes mixed nested structures")
        void decodesMixedStructures() {
            String toon = "user:\n  id: 123\n  name: Ada\n  tags[2]: reading,gaming\n  active: true";
            Map<String, Object> result = (Map<String, Object>) decode(toon);

            Map<String, Object> user = (Map<String, Object>) result.get("user");
            assertEquals(123L, user.get("id"));
            assertEquals("Ada", user.get("name"));
            assertEquals(true, user.get("active"));

            List<Object> tags = (List<Object>) user.get("tags");
            assertEquals(2, tags.size());
            assertEquals("reading", tags.get(0));
            assertEquals("gaming", tags.get(1));
        }
    }

    @Nested
    @DisplayName("Indentation")
    class Indentation {

        @Test
        @DisplayName("uses custom indentation")
        void usesCustomIndentation() {
            String toon = "outer:\n    value: 1";
            ToonOptions options = new ToonOptions(4, Delimiter.COMMA, false, true);

            Map<String, Object> result = (Map<String, Object>) decode(toon, options);
            Map<String, Object> outer = (Map<String, Object>) result.get("outer");
            assertEquals(1L, outer.get("value"));
        }
    }

    @Nested
    @DisplayName("Round-trip Encoding/Decoding")
    class RoundTrip {

        @Test
        @DisplayName("round-trip primitives")
        void roundTripPrimitives() {
            String encoded = Toon.encode("hello");
            Object decoded = decode(encoded);
            assertEquals("hello", decoded);
        }

        @Test
        @DisplayName("round-trip objects")
        void roundTripObjects() {
            Map<String, Object> original = Map.of("id", 1, "name", "Ada");
            String encoded = Toon.encode(original);
            Map<String, Object> decoded = (Map<String, Object>) decode(encoded);

            assertEquals(1, ((Number) decoded.get("id")).longValue());
            assertEquals("Ada", decoded.get("name"));
        }

        @Test
        @DisplayName("round-trip arrays")
        void roundTripArrays() {
            Map<String, Object> original = Map.of("tags", List.of("a", "b", "c"));
            String encoded = Toon.encode(original);
            Map<String, Object> decoded = (Map<String, Object>) decode(encoded);

            List<Object> tags = (List<Object>) decoded.get("tags");
            assertEquals(3, tags.size());
            assertEquals("a", tags.get(0));
            assertEquals("b", tags.get(1));
            assertEquals("c", tags.get(2));
        }
    }
}
