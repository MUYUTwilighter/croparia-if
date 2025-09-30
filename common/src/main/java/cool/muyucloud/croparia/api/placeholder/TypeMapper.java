package cool.muyucloud.croparia.api.placeholder;

import com.mojang.datafixers.util.Function3;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;

public interface TypeMapper<F, T> {
    static <T> TypeMapper<T, T> identity() {
        return (entry, placeholder, matcher) -> Optional.ofNullable(entry);
    }

    static <F, T> TypeMapper<F, T> of(Function<F, T> mapper) {
        return (entry, placeholder, matcher) -> Optional.ofNullable(mapper.apply(entry));
    }

    static <F, T> TypeMapper<F, T> of(BiFunction<F, String, T> mapper) {
        return (entry, placeholder, matcher) -> Optional.ofNullable(mapper.apply(entry, placeholder));
    }

    static <F, T> TypeMapper<F, T> of(Function3<F, String, Matcher, T> mapper) {
        return (entry, placeholder, matcher) -> Optional.ofNullable(mapper.apply(entry, placeholder, matcher));
    }

    Optional<T> map(F entry, String placeholder, Matcher matcher);
}
