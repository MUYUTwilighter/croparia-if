package cool.muyucloud.croparia.api.placeholder;

import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;

public class Template {
    public static final Codec<Template> CODEC = Codec.STRING.xmap(Template::new, Template::getTemplate);
    public static final Template EMPTY = new Template();

    private final String template;
    private final transient List<Span> spans = new ArrayList<>();

    public Template(String template) {
        this.template = template;
        this.read(template);
    }

    private Template() {
        this.template = "";
    }

    public String getTemplate() {
        return template;
    }

    public void read(String template) {
        spans.clear();
        boolean escape = false; // Whether the last character was a backslash
        boolean inSpan = false; // Whether currently inside a placeholder
        int spanStart = -1; // Start index of the current placeholder
        int depth = 0;  // Depth of nested braces
        for (int i = 0; i < template.length(); i++) {
            char c = template.charAt(i);
            // if the last character was a backslash, skip special processing
            if (escape) {
                escape = false;
                continue;
            }
            // Handle escape character
            if (c == '\\') {
                escape = true;
                continue;
            }
            // Handle placeholder detection
            if (!inSpan) {
                // Not currently in a placeholder, look for the start
                if (c == '$' && i + 1 < template.length() && template.charAt(i + 1) == '{') {
                    inSpan = true;
                    spanStart = i;  // Start of the placeholder
                    depth = 1;      // Initialize depth to 1 for the first '{'
                    i++;            // Skip the '{' character
                }
                // ignore other characters
            } else {
                // Currently in a placeholder, look for the end
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        // Found the end of the placeholder
                        int spanEnd = i + 1;
                        String content = template.substring(spanStart, spanEnd);
                        spans.add(new Span(spanStart, spanEnd, content));
                        inSpan = false;
                        spanStart = -1;
                    }
                }
                // ignore other characters
            }
        }
        if (inSpan) {
            throw new JsonParseException("Unclosed placeholder in template: " + template);
        }
    }

    public <T> String parse(T entry, Placeholder<T> placeholder) {
        return parse(entry, placeholder, Function.identity());
    }

    @SuppressWarnings("unchecked")
    public String parse(PlaceholderAccess entry) {
        return parse(entry, (Placeholder<PlaceholderAccess>) entry.placeholder(), Function.identity());
    }

    public <T> String parse(T entry, Placeholder<T> placeholder, Function<String, String> preProcess) {
        Map<String, String> cache = new HashMap<>();
        StringBuilder sb = new StringBuilder(template.length());
        int cursor = 0;
        for (Span span : spans) {
            String content = preProcess.apply(span.getContent());
            Matcher m = PatternKey.PLACEHOLDER.matcher(content);
            if (!m.matches()) {
                throw new RuntimeException("Malformed placeholder after being read: %s (%s)".formatted(span.getContent(), content));
            }
            String parsed = cache.computeIfAbsent(
                content,
                key -> placeholder.parseStart(entry, m.group(1), m)
            );
            // Append the text before the placeholder
            sb.append(template, cursor, span.getStart());
            sb.append(parsed);
            cursor = span.getEnd();
        }
        // The rest of the template after the last placeholder
        sb.append(template, cursor, template.length());
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Template template1)) return false;
        return Objects.equals(template, template1.template);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(template);
    }

    public boolean isEmpty() {
        return this.template.isEmpty();
    }

    public static class Span {
        private final int start;
        private final int end;
        private final String content;

        public Span(int start, int end, String content) {
            this.start = start;
            this.end = end;
            this.content = content;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public String getContent() {
            return content;
        }
    }
}
