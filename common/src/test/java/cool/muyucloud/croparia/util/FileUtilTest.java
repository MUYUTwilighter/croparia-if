package cool.muyucloud.croparia.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUtilTest {
    @TempDir
    Path tempDir;

    @Test
    void writeReadAndOverrideRulesWork() throws IOException {
        File file = tempDir.resolve("a/b/test.txt").toFile();

        FileUtil.write(file, "one", false);
        assertEquals("one", FileUtil.readUtf8(file));

        FileUtil.write(file, "two", false);
        assertEquals("one", FileUtil.readUtf8(file));

        FileUtil.write(file, "two", true);
        assertEquals("two", FileUtil.readUtf8(file));
    }

    @Test
    void transferHonorsOverrideFlag() throws IOException {
        File out = tempDir.resolve("transfer.txt").toFile();

        FileUtil.transfer(new ByteArrayInputStream("a".getBytes()), out, false);
        assertEquals("a", FileUtil.readUtf8(out));

        FileUtil.transfer(new ByteArrayInputStream("b".getBytes()), out, false);
        assertEquals("a", FileUtil.readUtf8(out));

        FileUtil.transfer(new ByteArrayInputStream("b".getBytes()), out, true);
        assertEquals("b", FileUtil.readUtf8(out));
    }

    @Test
    void forFilesInTraversesNestedFiles() throws IOException {
        File root = tempDir.resolve("root").toFile();
        FileUtil.write(new File(root, "a.txt"), "a", true);
        FileUtil.write(new File(root, "sub/b.txt"), "b", true);

        List<String> names = new ArrayList<>();
        FileUtil.forFilesIn(root, file -> names.add(file.getName()));

        assertTrue(names.contains("a.txt"));
        assertTrue(names.contains("b.txt"));
        assertEquals(2, names.size());
    }

    @Test
    void deleteUtilitiesRemoveExpectedPaths() throws IOException {
        File root = tempDir.resolve("delete").toFile();
        File child = new File(root, "c.txt");
        File nested = new File(root, "x/y/z.txt");
        FileUtil.write(child, "c", true);
        FileUtil.write(nested, "z", true);

        assertTrue(FileUtil.deleteIfExists(child));
        assertFalse(child.exists());
        assertFalse(FileUtil.deleteIfExists(child));

        FileUtil.deleteUnder(root);
        assertTrue(root.exists());
        assertEquals(0, root.listFiles().length);

        FileUtil.deleteDir(root);
        assertFalse(root.exists());
    }

    @Test
    void extensionHandlesEdgeCases() {
        assertEquals("txt", FileUtil.extension("a.txt"));
        assertEquals("", FileUtil.extension("a"));
        assertEquals("", FileUtil.extension("a."));
    }

    @Test
    void ensureDirectoryAndForFilesInFailOnRegularFilePath() throws IOException {
        File file = tempDir.resolve("plain.txt").toFile();
        FileUtil.write(file, "x", true);

        assertThrows(IOException.class, () -> FileUtil.ensureDirectory(file));
        assertThrows(IOException.class, () -> FileUtil.forFilesIn(file, f -> {}));
    }

    @Test
    void deleteUnderIgnoresRegularFile() throws IOException {
        File file = tempDir.resolve("single.txt").toFile();
        FileUtil.write(file, "x", true);

        FileUtil.deleteUnder(file);
        assertTrue(file.exists());
    }
}
