package cool.muyucloud.croparia.api.generator.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JarJarEntryTest {
    @Test
    void forInputStreamReadsExpectedJarEntryContent(@TempDir Path tempDir) throws IOException {
        Path jarPath = tempDir.resolve("sample.jar");
        String entryName = "data-generators/croparia/test/a.json";
        byte[] content = "{\"k\":1}".getBytes(StandardCharsets.UTF_8);

        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jarPath))) {
            JarEntry entry = new JarEntry(entryName);
            out.putNextEntry(entry);
            out.write(content);
            out.closeEntry();
        }

        AtomicReference<String> read = new AtomicReference<>();
        JarJarEntry jarJarEntry = new JarJarEntry(jarPath.toFile(), new JarEntry(entryName));
        jarJarEntry.forInputStream(stream -> read.set(readUtf8(stream)));

        assertEquals("{\"k\":1}", read.get());
        assertTrue(jarJarEntry.getJarFile().isFile());
        assertEquals(entryName, jarJarEntry.getJarEntry().getName());
    }

    private static String readUtf8(InputStream stream) throws IOException {
        return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
}

