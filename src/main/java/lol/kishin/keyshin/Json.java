package lol.kishin.keyshin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tiny JSON reader/writer so the library needs no dependencies. Internal use only. */
final class Json {

    private final String s;
    private int i;

    private Json(String s) {
        this.s = s;
    }

    /** Parses a JSON object. Throws IllegalArgumentException on anything malformed. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> parseObject(String text) {
        if (text == null) {
            throw new IllegalArgumentException("empty response");
        }
        try {
            Json p = new Json(text);
            p.ws();
            if (!p.peek('{')) {
                throw p.err();
            }
            Map<String, Object> result = (Map<String, Object>) p.value();
            p.ws();
            if (p.i != p.s.length()) {
                throw p.err();
            }
            return result;
        } catch (StringIndexOutOfBoundsException | NumberFormatException e) {
            throw new IllegalArgumentException("malformed JSON", e);
        }
    }

    static String quote(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 2).append('"');
        for (int k = 0; k < value.length(); k++) {
            char c = value.charAt(k);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    private Object value() {
        ws();
        if (i >= s.length()) {
            throw err();
        }
        return switch (s.charAt(i)) {
            case '{' -> object();
            case '[' -> array();
            case '"' -> string();
            case 't' -> literal("true", Boolean.TRUE);
            case 'f' -> literal("false", Boolean.FALSE);
            case 'n' -> literal("null", null);
            default -> number();
        };
    }

    private Map<String, Object> object() {
        Map<String, Object> map = new LinkedHashMap<>();
        i++;
        ws();
        if (peek('}')) {
            i++;
            return map;
        }
        while (true) {
            ws();
            if (!peek('"')) {
                throw err();
            }
            String key = string();
            ws();
            expect(':');
            map.put(key, value());
            ws();
            if (peek(',')) {
                i++;
                continue;
            }
            expect('}');
            return map;
        }
    }

    private List<Object> array() {
        List<Object> list = new ArrayList<>();
        i++;
        ws();
        if (peek(']')) {
            i++;
            return list;
        }
        while (true) {
            list.add(value());
            ws();
            if (peek(',')) {
                i++;
                continue;
            }
            expect(']');
            return list;
        }
    }

    private String string() {
        StringBuilder sb = new StringBuilder();
        i++; // opening quote
        while (i < s.length()) {
            char c = s.charAt(i++);
            if (c == '"') {
                return sb.toString();
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            char e = s.charAt(i++);
            switch (e) {
                case '"', '\\', '/' -> sb.append(e);
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                case 't' -> sb.append('\t');
                case 'u' -> {
                    sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                    i += 4;
                }
                default -> throw err();
            }
        }
        throw err();
    }

    private Number number() {
        int start = i;
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) {
            i++;
        }
        String n = s.substring(start, i);
        if (n.isEmpty()) {
            throw err();
        }
        if (n.indexOf('.') >= 0 || n.indexOf('e') >= 0 || n.indexOf('E') >= 0) {
            return Double.parseDouble(n);
        }
        return Long.parseLong(n);
    }

    private Object literal(String word, Object result) {
        if (!s.startsWith(word, i)) {
            throw err();
        }
        i += word.length();
        return result;
    }

    private void ws() {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
    }

    private boolean peek(char c) {
        return i < s.length() && s.charAt(i) == c;
    }

    private void expect(char c) {
        if (!peek(c)) {
            throw err();
        }
        i++;
    }

    private IllegalArgumentException err() {
        return new IllegalArgumentException("malformed JSON at position " + i);
    }
}
