package io.github.koinsaari.jtoon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.github.koinsaari.jtoon.ToonOptions.Delimiter;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ToonEncoderTest {

    private static String encode(Object value) {
        return Toon.encode(value);
    }

    private static String encode(Object value, ToonOptions options) {
        return Toon.encode(value, options);
    }

    @Nested
    @DisplayName("Primitives")
    class Primitives {

        @Test
        @DisplayName("encodes null")
        void encodesNull() {
            assertEquals("null", encode(null));
        }

        @Test
        @DisplayName("encodes booleans")
        void encodesBooleans() {
            assertEquals("true", encode(true));
            assertEquals("false", encode(false));
        }

        @Test
        @DisplayName("encodes integers")
        void encodesIntegers() {
            assertEquals("0", encode(0));
            assertEquals("42", encode(42));
            assertEquals("-7", encode(-7));
        }

        @Test
        @DisplayName("encodes floating point numbers")
        void encodesFloatingPoint() {
            assertEquals("3.14", encode(3.14));
            assertEquals("-0.5", encode(-0.5));
        }

        @Test
        @DisplayName("normalizes negative zero to zero")
        void normalizesNegativeZero() {
            assertEquals("0", encode(-0.0));
        }

        @Test
        @DisplayName("encodes safe strings without quotes")
        void encodesSafeStrings() {
            assertEquals("hello", encode("hello"));
            assertEquals("Ada_99", encode("Ada_99"));
            assertEquals("hello world", encode("hello world"));
        }

        @Test
        @DisplayName("quotes empty string")
        void quotesEmptyString() {
            assertEquals("\"\"", encode(""));
        }

        @Test
        @DisplayName("quotes strings with leading or trailing spaces")
        void quotesWhitespace() {
            assertEquals("\" padded \"", encode(" padded "));
            assertEquals("\"  \"", encode("  "));
        }

        @Test
        @DisplayName("quotes strings that look like booleans or numbers")
        void quotesAmbiguousStrings() {
            assertEquals("\"true\"", encode("true"));
            assertEquals("\"false\"", encode("false"));
            assertEquals("\"null\"", encode("null"));
            assertEquals("\"42\"", encode("42"));
            assertEquals("\"-3.14\"", encode("-3.14"));
        }

        @Test
        @DisplayName("escapes control characters")
        void escapesControlChars() {
            assertEquals("\"line1\\nline2\"", encode("line1\nline2"));
            assertEquals("\"tab\\there\"", encode("tab\there"));
            assertEquals("\"return\\rcarriage\"", encode("return\rcarriage"));
        }

        @Test
        @DisplayName("escapes backslashes and quotes")
        void escapesSpecialChars() {
            assertEquals("\"C:\\\\Users\\\\path\"", encode("C:\\Users\\path"));
            assertEquals("\"say \\\"hello\\\"\"", encode("say \"hello\""));
        }

        @Test
        @DisplayName("quotes strings with structural meanings")
        void quotesStructuralStrings() {
            assertEquals("\"[5]\"", encode("[5]"));
            assertEquals("\"- item\"", encode("- item"));
            assertEquals("\"{key}\"", encode("{key}"));
        }
    }

    @Nested
    @DisplayName("Objects")
    class Objects {

        @Test
        @DisplayName("encodes simple object")
        void encodesSimpleObject() {
            Map<String, Object> obj = new LinkedHashMap<>();
            obj.put("id", 123);
            obj.put("name", "Ada");
            obj.put("active", true);
            assertEquals("id: 123\nname: Ada\nactive: true", encode(obj));
        }

        @Test
        @DisplayName("encodes empty object")
        void encodesEmptyObject() {
            assertEquals("", encode(Map.of()));
        }

        @Test
        @DisplayName("encodes nested objects")
        void encodesNestedObjects() {
            Map<String, Object> inner = new LinkedHashMap<>();
            inner.put("city", "Springfield");
            inner.put("zip", "12345");

            Map<String, Object> outer = new LinkedHashMap<>();
            outer.put("name", "Alice");
            outer.put("address", inner);

            assertEquals("""
                name: Alice
                address:
                  city: Springfield
                  zip: "12345\"""", encode(outer));
        }

        @Test
        @DisplayName("encodes deeply nested objects")
        void encodesDeeplyNestedObjects() {
            Map<String, Object> deep = Map.of("value", "deep");
            Map<String, Object> mid = Map.of("c", deep);
            Map<String, Object> obj = Map.of("a", mid);

            assertEquals("a:\n  c:\n    value: deep", encode(obj));
        }

        @Test
        @DisplayName("quotes keys with special characters")
        void quotesSpecialKeys() {
            Map<String, Object> obj = new LinkedHashMap<>();
            obj.put("order:id", 7);
            obj.put("full name", "Ada");
            obj.put("", 1);

            assertEquals("\"order:id\": 7\n\"full name\": Ada\n\"\": 1", encode(obj));
        }

        @Test
        @DisplayName("quotes string values with special characters")
        void quotesSpecialValues() {
            Map<String, Object> obj = new LinkedHashMap<>();
            obj.put("note", "a:b");
            obj.put("csv", "a,b");

            assertEquals("note: \"a:b\"\ncsv: \"a,b\"", encode(obj));
        }
    }

    @Nested
    @DisplayName("Arrays of Primitives")
    class PrimitiveArrays {

        @Test
        @DisplayName("encodes inline primitive array")
        void encodesInlineArray() {
            Map<String, Object> obj = Map.of("tags", List.of("reading", "gaming", "coding"));
            assertEquals("tags[3]: reading,gaming,coding", encode(obj));
        }

        @Test
        @DisplayName("encodes empty array")
        void encodesEmptyArray() {
            Map<String, Object> obj = Map.of("items", List.of());
            assertEquals("items[0]:", encode(obj));
        }

        @Test
        @DisplayName("encodes mixed primitive types")
        void encodesMixedPrimitives() {
            List<Object> list = new ArrayList<>();
            list.add("text");
            list.add(42);
            list.add(true);
            list.add(null);
            Map<String, Object> obj = Map.of("data", list);
            assertEquals("data[4]: text,42,true,null", encode(obj));
        }

        @Test
        @DisplayName("quotes array strings with special characters")
        void quotesSpecialStrings() {
            Map<String, Object> obj = Map.of("items", List.of("a", "b,c", "d:e"));
            assertEquals("items[3]: a,\"b,c\",\"d:e\"", encode(obj));
        }

        @Test
        @DisplayName("handles empty strings in arrays")
        void handlesEmptyStrings() {
            Map<String, Object> obj = Map.of("items", java.util.Arrays.asList("a", "", "b"));
            assertEquals("items[3]: a,\"\",b", encode(obj));
        }

        @Test
        @DisplayName("encodes root-level primitive array")
        void encodesRootArray() {
            assertEquals("[3]: x,y,z", encode(List.of("x", "y", "z")));
        }
    }

    @Nested
    @DisplayName("Arrays of Objects - Tabular Format")
    class TabularArrays {

        @Test
        @DisplayName("encodes uniform objects in tabular format")
        void encodesTabularFormat() {
            Map<String, Object> row1 = new LinkedHashMap<>();
            row1.put("id", 1);
            row1.put("name", "Alice");
            row1.put("role", "admin");

            Map<String, Object> row2 = new LinkedHashMap<>();
            row2.put("id", 2);
            row2.put("name", "Bob");
            row2.put("role", "user");

            Map<String, Object> obj = Map.of("users", List.of(row1, row2));

            assertEquals("""
                users[2]{id,name,role}:
                  1,Alice,admin
                  2,Bob,user""", encode(obj));
        }

        @Test
        @DisplayName("handles null values in tabular format")
        void handlesNullInTabular() {
            Map<String, Object> row1 = new LinkedHashMap<>();
            row1.put("id", 1);
            row1.put("value", null);

            Map<String, Object> row2 = new LinkedHashMap<>();
            row2.put("id", 2);
            row2.put("value", "test");

            Map<String, Object> obj = Map.of("items", List.of(row1, row2));

            assertEquals("""
                items[2]{id,value}:
                  1,null
                  2,test""", encode(obj));
        }

        @Test
        @DisplayName("quotes strings with delimiters in tabular rows")
        void quotesDelimitersInTabular() {
            Map<String, Object> row1 = new LinkedHashMap<>();
            row1.put("sku", "A,1");
            row1.put("desc", "cool");

            Map<String, Object> row2 = new LinkedHashMap<>();
            row2.put("sku", "B2");
            row2.put("desc", "test:value");

            Map<String, Object> obj = Map.of("items", List.of(row1, row2));

            assertEquals("""
                items[2]{sku,desc}:
                  "A,1",cool
                  B2,"test:value\"""", encode(obj));
        }

        @Test
        @DisplayName("uses tabular format for root-level array")
        void encodesRootTabular() {
            Map<String, Object> row1 = new LinkedHashMap<>();
            row1.put("id", 1);
            row1.put("name", "Alice");

            Map<String, Object> row2 = new LinkedHashMap<>();
            row2.put("id", 2);
            row2.put("name", "Bob");

            assertEquals("""
                [2]{id,name}:
                  1,Alice
                  2,Bob""", encode(List.of(row1, row2)));
        }

        @Test
        @DisplayName("handles single column tabular format")
        void handlesSingleColumn() {
            Map<String, Object> row1 = Map.of("id", 1);
            Map<String, Object> row2 = Map.of("id", 2);

            Map<String, Object> obj = Map.of("items", List.of(row1, row2));

            assertEquals("""
                items[2]{id}:
                  1
                  2""", encode(obj));
        }
    }

    @Nested
    @DisplayName("Arrays of Objects - List Format")
    class ListArrays {

        @Test
        @DisplayName("uses list format for non-uniform objects")
        void usesListForNonUniform() {
            Map<String, Object> item1 = new LinkedHashMap<>();
            item1.put("id", 1);
            item1.put("name", "First");

            Map<String, Object> item2 = new LinkedHashMap<>();
            item2.put("id", 2);
            item2.put("name", "Second");
            item2.put("extra", true);

            Map<String, Object> obj = Map.of("items", List.of(item1, item2));

            assertEquals("""
                items[2]:
                  - id: 1
                    name: First
                  - id: 2
                    name: Second
                    extra: true""", encode(obj));
        }

        @Test
        @DisplayName("uses list format for objects with nested values")
        void usesListForNested() {
            Map<String, Object> nested = Map.of("x", 1);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", 1);
            item.put("nested", nested);

            Map<String, Object> obj = Map.of("items", List.of(item));

            assertEquals("""
                items[1]:
                  - id: 1
                    nested:
                      x: 1""", encode(obj));
        }

        @Test
        @DisplayName("uses list format for mixed types")
        void usesListForMixedTypes() {
            Map<String, Object> item = Map.of("a", 1);
            Map<String, Object> obj = Map.of("items", List.of(1, item, "text"));

            assertEquals("""
                items[3]:
                  - 1
                  - a: 1
                  - text""", encode(obj));
        }
    }

    @Nested
    @DisplayName("Delimiter Options")
    class DelimiterOptions {

        @Test
        @DisplayName("encodes with tab delimiter")
        void encodesWithTab() {
            Map<String, Object> obj = Map.of("tags", List.of("a", "b", "c"));
            ToonOptions options = new ToonOptions(2, Delimiter.TAB, false, true);

            assertEquals("tags[3\t]: a\tb\tc", encode(obj, options));
        }

        @Test
        @DisplayName("encodes with pipe delimiter")
        void encodesWithPipe() {
            Map<String, Object> obj = Map.of("tags", List.of("a", "b", "c"));
            ToonOptions options = new ToonOptions(2, Delimiter.PIPE, false, true);

            assertEquals("tags[3|]: a|b|c", encode(obj, options));
        }

        @Test
        @DisplayName("encodes tabular array with tab delimiter")
        void encodesTabularWithTab() {
            Map<String, Object> row1 = new LinkedHashMap<>();
            row1.put("id", 1);
            row1.put("name", "Alice");

            Map<String, Object> row2 = new LinkedHashMap<>();
            row2.put("id", 2);
            row2.put("name", "Bob");

            Map<String, Object> obj = Map.of("users", List.of(row1, row2));
            ToonOptions options = new ToonOptions(2, Delimiter.TAB, false, true);

            assertEquals("users[2\t]{id\tname}:\n  1\tAlice\n  2\tBob", encode(obj, options));
        }

        @Test
        @DisplayName("does not quote commas with non-comma delimiter")
        void doesNotQuoteCommasWithTab() {
            Map<String, Object> obj = Map.of("items", List.of("a,b", "c,d"));
            ToonOptions options = new ToonOptions(2, Delimiter.TAB, false, true);

            assertEquals("items[2\t]: a,b\tc,d", encode(obj, options));
        }

        @Test
        @DisplayName("quotes delimiter character in values")
        void quotesDelimiterInValues() {
            Map<String, Object> obj = Map.of("items", List.of("a", "b|c"));
            ToonOptions options = new ToonOptions(2, Delimiter.PIPE, false, true);

            assertEquals("items[2|]: a|\"b|c\"", encode(obj, options));
        }
    }

    @Nested
    @DisplayName("Length Marker Option")
    class LengthMarkerOption {

        @Test
        @DisplayName("adds length marker to arrays")
        void addsLengthMarker() {
            Map<String, Object> obj = Map.of("tags", List.of("a", "b", "c"));
            ToonOptions options = new ToonOptions(2, Delimiter.COMMA, true, true);

            assertEquals("tags[#3]: a,b,c", encode(obj, options));
        }

        @Test
        @DisplayName("adds length marker to tabular arrays")
        void addsLengthMarkerToTabular() {
            Map<String, Object> row1 = new LinkedHashMap<>();
            row1.put("id", 1);
            Map<String, Object> row2 = new LinkedHashMap<>();
            row2.put("id", 2);

            Map<String, Object> obj = Map.of("items", List.of(row1, row2));
            ToonOptions options = new ToonOptions(2, Delimiter.COMMA, true, true);

            assertEquals("items[#2]{id}:\n  1\n  2", encode(obj, options));
        }

        @Test
        @DisplayName("adds length marker with custom delimiter")
        void addsLengthMarkerWithDelimiter() {
            Map<String, Object> obj = Map.of("tags", List.of("a", "b"));
            ToonOptions options = new ToonOptions(2, Delimiter.PIPE, true, true);

            assertEquals("tags[#2|]: a|b", encode(obj, options));
        }

        @Test
        @DisplayName("adds length marker to empty arrays")
        void addsLengthMarkerToEmpty() {
            Map<String, Object> obj = Map.of("items", List.of());
            ToonOptions options = new ToonOptions(2, Delimiter.COMMA, true, true);

            assertEquals("items[#0]:", encode(obj, options));
        }
    }

    @Nested
    @DisplayName("Complex Structures")
    class ComplexStructures {

        @Test
        @DisplayName("encodes mixed nested structures")
        void encodesMixedStructures() {
            Map<String, Object> user = new LinkedHashMap<>();
            user.put("id", 123);
            user.put("name", "Ada");
            user.put("tags", List.of("reading", "gaming"));
            user.put("active", true);

            Map<String, Object> obj = Map.of("user", user);

            assertEquals("""
                user:
                  id: 123
                  name: Ada
                  tags[2]: reading,gaming
                  active: true""", encode(obj));
        }

        @Test
        @DisplayName("encodes nested tabular arrays")
        void encodesNestedTabular() {
            Map<String, Object> innerRow1 = new LinkedHashMap<>();
            innerRow1.put("x", 1);
            innerRow1.put("y", 2);

            Map<String, Object> innerRow2 = new LinkedHashMap<>();
            innerRow2.put("x", 3);
            innerRow2.put("y", 4);

            Map<String, Object> outerRow = new LinkedHashMap<>();
            outerRow.put("id", 1);
            outerRow.put("points", List.of(innerRow1, innerRow2));

            Map<String, Object> obj = Map.of("data", List.of(outerRow));

            assertEquals("""
                data[1]:
                  - id: 1
                    points[2]{x,y}:
                      1,2
                      3,4""", encode(obj));
        }
    }

    @Nested
    @DisplayName("Indentation")
    class Indentation {

        @Test
        @DisplayName("uses custom indentation")
        void usesCustomIndentation() {
            Map<String, Object> inner = Map.of("value", 1);
            Map<String, Object> obj = Map.of("outer", inner);

            ToonOptions options = new ToonOptions(4, Delimiter.COMMA, false, true);

            assertEquals("outer:\n    value: 1", encode(obj, options));
        }
    }
}
