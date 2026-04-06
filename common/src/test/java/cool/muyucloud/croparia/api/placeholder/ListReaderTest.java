package cool.muyucloud.croparia.api.placeholder;

import com.google.gson.JsonArray;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ListReaderTest {
    @Test
    void collectionReaderUsesImmutableSnapshot() {
        List<String> source = new ArrayList<>();
        source.add("a");
        source.add("b");

        ListReader<String> reader = ListReader.collection(source);
        source.add("c");

        assertEquals(2, reader.size());
        assertEquals("a", reader.get(0));
        assertEquals("b", reader.get(1));
    }

    @Test
    void listReaderReflectsBackingListChanges() {
        List<String> source = new ArrayList<>();
        source.add("x");

        ListReader<String> reader = ListReader.list(source);
        source.add("y");

        assertEquals(2, reader.size());
        assertEquals("y", reader.get(1));
    }

    @Test
    void arrayReaderSupportsIndexAndIteration() {
        ListReader.ArrayReader<Integer> reader = new ListReader.ArrayReader<>(new Integer[]{1, 2, 3});

        assertEquals(3, reader.size());
        assertEquals(2, reader.get(1));

        int sum = 0;
        for (int value : reader) {
            sum += value;
        }
        assertEquals(6, sum);
    }

    @Test
    void jsonArrayReaderExposesJsonElements() {
        JsonArray array = new JsonArray();
        array.add("name");
        array.add(7);
        ListReader.JsonArrayReader reader = ListReader.jsonArray(array);

        assertEquals(2, reader.size());
        assertEquals("name", reader.get(0).getAsString());
        assertEquals(7, reader.get(1).getAsInt());
        assertTrue(reader.iterator().hasNext());
    }

    @Test
    void iteratorHasNextTracksBounds() {
        ListReader<String> reader = ListReader.list(List.of("a"));
        var iterator = reader.iterator();
        assertTrue(iterator.hasNext());
        assertEquals("a", iterator.next());
        assertFalse(iterator.hasNext());
    }
}
