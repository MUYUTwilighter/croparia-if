package cool.muyucloud.croparia.api.generator.util;

import com.google.gson.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;

public class DgCompiler {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static JsonObject compile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = fis.readAllBytes();
            String content = new String(data);
            return compile(content);
        }
    }

    /**
     * Compile a CDG-formatted string into a JsonObject.
     */
    public static JsonObject compile(String source) {
        if (source == null) source = "";
        int len = source.length();
        int i = 0;
        JsonObject root = new JsonObject();

        // skip bom and blank
        if (source.startsWith("\uFEFF")) i += 1;

        while (true) {
            i = skipWhitespace(source, i);
            if (i >= len) {
                // no template
                return root;
            }
            char c = source.charAt(i);
            if (c != '@') {
                // template
                String template = source.substring(i).trim();
                if (!template.isEmpty()) {
                    root.addProperty("template", template);
                }
                return root;
            }

            // analyze @meta=value;
            i++; // skip '@'
            int keyStart = i;
            // read until blank or '='
            while (i < len) {
                char kc = source.charAt(i);
                if (kc == '=' || Character.isWhitespace(kc)) break;
                i++;
            }
            String key = source.substring(keyStart, i).trim();
            if (key.isEmpty()) {
                throw syntax("期待元数据键名，如 @meta=...", source, i);
            }
            i = skipWhitespace(source, i);
            if (i >= len || source.charAt(i) != '=') {
                throw syntax("缺少 '='", source, i);
            }
            i++; // skip '='
            i = skipWhitespace(source, i);
            if (i >= len) {
                throw syntax("缺少值", source, i);
            }

            // parse meta value
            ParseResult val = readValueUntilSemicolon(source, i);
            i = val.nextIndex;
            i = skipWhitespace(source, i);
            if (i >= len || source.charAt(i) != ';') {
                throw syntax("元数据项缺少结束分号 ';'", source, i);
            }
            i++; // consume ';'

            // dump to json
            root.add(key, toJsonElement(val.value, val.kind));
        }
    }

    /**
     * read value string to JsonElement according to its kind
     */
    private static JsonElement toJsonElement(String raw, ValueKind kind) {
        try {
            switch (kind) {
                case MULTILINE:
                case STRING:
                    return new JsonPrimitive(raw);
                case JSON: {
                    return GSON.fromJson(raw, JsonElement.class);
                }
                case AMBIGUOUS: {
                    try {
                        return GSON.fromJson(raw, JsonElement.class);
                    } catch (JsonSyntaxException ex) {
                        return new JsonPrimitive(raw);
                    }
                }
            }
        } catch (JsonSyntaxException ignored) {
        }
        return new JsonPrimitive(raw);
    }

    /**
     * read meta value until a real semicolon ';' (not in string or structure)
     */
    private static ParseResult readValueUntilSemicolon(String s, int i) {
        final int len = s.length();
        if (i >= len) return new ParseResult("", ValueKind.STRING, i);

        // triple single-quoted multi-line string
        if (peekTripleQuote(s, i)) {
            int start = i + 3;
            int end = indexOfTripleQuote(s, start);
            if (end < 0) throw syntax("Unclosed triple-quoted string (''')", s, i);
            String content = s.substring(start, end);
            return new ParseResult(content, ValueKind.MULTILINE, end + 3);
        }

        char ch = s.charAt(i);
        // single/double-quoted string
        if (ch == '"' || ch == '\'') {
            boolean isDouble = (ch == '"');
            StringBuilder out = new StringBuilder();
            int j = i + 1;
            while (j < len) {
                char c = s.charAt(j++);
                if (c == '\\') {
                    if (j >= len) break;
                    char e = s.charAt(j++);
                    // escape sequences
                    switch (e) {
                        case 'n':
                            out.append('\n');
                            break;
                        case 'r':
                            out.append('\r');
                            break;
                        case 't':
                            out.append('\t');
                            break;
                        case 'b':
                            out.append('\b');
                            break;
                        case 'f':
                            out.append('\f');
                            break;
                        case '\\':
                            out.append('\\');
                            break;
                        case '\'':
                            out.append('\'');
                            break;
                        case '"':
                            out.append('"');
                            break;
                        case 'u': {
                            if (j + 3 < len) {
                                String hex = s.substring(j, j + 4);
                                try {
                                    out.append((char) Integer.parseInt(hex, 16));
                                    j += 4;
                                } catch (NumberFormatException nfe) {
                                    out.append("\\u").append(hex);
                                    j += 4;
                                }
                            } else {
                                out.append("\\u");
                            }
                            break;
                        }
                        default:
                            out.append(e);
                    }
                } else if (c == (isDouble ? '"' : '\'')) {
                    return new ParseResult(out.toString(), ValueKind.STRING, j);
                } else {
                    out.append(c);
                }
            }
            throw syntax("Unclosed quotes", s, i);
        }

        // JSON/String
        int j = i;
        int brace = 0;   // {}
        int bracket = 0; // []
        boolean inStr = false;
        char strQuote = 0;
        boolean escape = false;

        while (j < len) {
            char c = s.charAt(j);
            if (inStr) {
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == strQuote) {
                    inStr = false;
                }
                j++;
                continue;
            }

            switch (c) {
                case '"':
                case '\'':
                    inStr = true;
                    strQuote = c;
                    j++;
                    break;
                case '{':
                    brace++;
                    j++;
                    break;
                case '}':
                    if (brace > 0) brace--;
                    j++;
                    break;
                case '[':
                    bracket++;
                    j++;
                    break;
                case ']':
                    if (bracket > 0) bracket--;
                    j++;
                    break;
                case ';':
                    // read until real semicolon
                    if (brace == 0 && bracket == 0) {
                        String raw = s.substring(i, j).trim();
                        // JSON/AMBIGUOUS
                        ValueKind kind = guessKind(raw);
                        return new ParseResult(raw, kind, j);
                    }
                    j++;
                    break;
                default:
                    j++;
            }
        }

        // no semicolon found
        throw syntax("Semicolon ';' not found ", s, i);
    }

    private static ValueKind guessKind(String raw) {
        if (raw.isEmpty()) return ValueKind.STRING;
        char f = raw.charAt(0);
        // compound, array, string
        if (f == '{' || f == '[' || f == '"') return ValueKind.JSON;
        // other JSON primitive: number, boolean, null
        if (f == '-' || (f >= '0' && f <= '9') || f == 't' || f == 'f' || f == 'n') {
            return ValueKind.AMBIGUOUS;
        }
        return ValueKind.STRING;
    }

    private static boolean peekTripleQuote(String s, int i) {
        return i + 2 < s.length() && s.charAt(i) == '\'' && s.charAt(i + 1) == '\'' && s.charAt(i + 2) == '\'';
    }

    private static int indexOfTripleQuote(String s, int from) {
        int i = s.indexOf("'''", from);
        if (i < 0) return -1;
        return i;
    }

    private static int skipWhitespace(String s, int i) {
        int len = s.length();
        while (i < len) {
            char c = s.charAt(i);
            if (!Character.isWhitespace(c)) break;
            i++;
        }
        return i;
    }

    private static IllegalArgumentException syntax(String msg, String src, int pos) {
        // show context
        int start = Math.max(0, pos - 20);
        int end = Math.min(src.length(), pos + 20);
        String ctx = src.substring(start, end).replace("\n", "\\n");
        return new IllegalArgumentException(msg + ", at=" + pos + ", context: ..." + ctx + "...");
    }

    private enum ValueKind {
        STRING,       // single/double-quoted string
        MULTILINE,    // triple single quoted multi-line string
        JSON,         // compound or array
        AMBIGUOUS     // possibly compound or array, or others
    }

    private static class ParseResult {
        final String value;
        final ValueKind kind;
        final int nextIndex;

        ParseResult(String v, ValueKind k, int n) {
            this.value = Objects.requireNonNull(v);
            this.kind = Objects.requireNonNull(k);
            this.nextIndex = n;
        }
    }
}