package cool.muyucloud.croparia.api.placeholder;

public interface PlaceholderFactory<T> {
    static <T> PlaceholderFactory<T> identity() {
        return builder -> builder;
    }

    PlaceholderBuilder<T> apply(PlaceholderBuilder<T> builder);
}
