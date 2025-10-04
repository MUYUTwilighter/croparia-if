package cool.muyucloud.croparia.api.placeholder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public interface ListReader<E> extends Iterable<E> {
    static JsonArrayReader jsonArray(JsonArray array) {
        return new JsonArrayReader(array);
    }

    static <E> ListReaderImpl<E> collection(Collection<E> list) {
        return new ListReaderImpl<>(List.copyOf(list));
    }

    static <E> ListReaderImpl<E> list(List<E> list) {
        return new ListReaderImpl<>(list);
    }

    int size();

    E get(int index);

    @Override
    default @NotNull Iterator<E> iterator() {
        return new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < size();
            }

            @Override
            public E next() {
                if (!hasNext()) {
                    throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
                }
                return get(index++);
            }
        };
    }

    class JsonArrayReader implements ListReader<JsonElement> {
        private final JsonArray array;

        public JsonArrayReader(JsonArray array) {
            this.array = array;
        }

        public JsonArray get() {
            return array;
        }

        @Override
        public int size() {
            return this.get().size();
        }

        @Override
        public JsonElement get(int index) {
            return this.get().get(index);
        }

        @Override
        public @NotNull Iterator<JsonElement> iterator() {
            return this.get().iterator();
        }
    }

    @SuppressWarnings("unused")
    class ListReaderImpl<E> implements ListReader<E> {
        private final java.util.List<E> list;

        public ListReaderImpl(java.util.List<E> list) {
            this.list = list;
        }

        public List<E> get() {
            return list;
        }

        @Override
        public int size() {
            return this.get().size();
        }

        @Override
        public E get(int index) {
            return this.get().get(index);
        }

        @Override
        public @NotNull Iterator<E> iterator() {
            return this.get().iterator();
        }
    }

    @SuppressWarnings("unused")
    class ArrayReader<E> implements ListReader<E> {
        private final E[] array;

        public ArrayReader(E[] array) {
            this.array = array;
        }

        public E[] get() {
            return array;
        }

        @Override
        public int size() {
            return this.get().length;
        }

        @Override
        public E get(int index) {
            return this.get()[index];
        }

        @Override
        public @NotNull Iterator<E> iterator() {
            return Stream.of(this.get()).iterator();
        }
    }
}
