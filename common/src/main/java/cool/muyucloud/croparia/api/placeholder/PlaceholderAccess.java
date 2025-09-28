package cool.muyucloud.croparia.api.placeholder;

import java.util.regex.Matcher;

public interface PlaceholderAccess {
    static <T> Direct<T> of(T value, Placeholder<T> placeholder) {
        return new Direct<>(value, placeholder);
    }

    Placeholder<?> placeholder();

    @SuppressWarnings("unchecked")
    default String parsePlaceholder(String placeholder, Matcher matcher) throws RegexParserException {
        try {
            return ((Placeholder<PlaceholderAccess>) this.placeholder()).parseStart(this, placeholder, matcher);
        } catch (ClassCastException e) {
            throw new RegexParserException(e);
        }
    }

    class Direct<T> implements PlaceholderAccess {
        private final T value;
        private final Placeholder<T> placeholder;

        private Direct(T value, Placeholder<T> placeholder) {
            this.value = value;
            this.placeholder = placeholder;
        }

        @Override
        public Placeholder<T> placeholder() {
            return this.placeholder;
        }

        @Override
        public String parsePlaceholder(String placeholder, Matcher matcher) throws RegexParserException {
            try {
                return this.placeholder().parseStart(this.value, placeholder, matcher);
            } catch (ClassCastException e) {
                throw new RegexParserException(e);
            }
        }
    }
}
