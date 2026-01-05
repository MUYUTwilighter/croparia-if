package cool.muyucloud.croparia.api.generator.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarJarEntry {
    private final File jarFile;
    private final JarEntry entry;

    public JarJarEntry(File jarFile, JarEntry entry) {
        this.jarFile = jarFile;
        this.entry = entry;
    }

    public File getJarFile() {
        return jarFile;
    }

    public JarEntry getJarEntry() {
        return entry;
    }

    /**
     * Performs the given action with the input stream of the jar entry.
     *
     * @param consumer the action to be performed with the input stream
     * @throws IOException       if an I/O error occurs
     */
    public void forInputStream(InputStreamConsumer consumer) throws IOException {
        try (JarFile jar = new JarFile(this.getJarFile())) {
            try (InputStream stream = jar.getInputStream(this.getJarEntry())) {
                consumer.accept(stream);
            }
        }
    }

    public interface InputStreamConsumer {
        void accept(InputStream stream) throws IOException;
    }
}
