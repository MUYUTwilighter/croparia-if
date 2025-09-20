package cool.muyucloud.croparia.api.generator.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarJarEntry {
    private final File file;
    private final JarEntry entry;

    public JarJarEntry(File file, JarEntry entry) {
        this.file = file;
        this.entry = entry;
    }

    public File getFile() {
        return file;
    }

    public JarEntry getEntry() {
        return entry;
    }

    public void forInputStream(InputStreamConsumer consumer) throws IOException {
        try (JarFile jar = new JarFile(this.getFile())) {
            try (InputStream stream = jar.getInputStream(this.getEntry())) {
                consumer.accept(stream);
            }
        }
    }

    public interface InputStreamConsumer {
        void accept(InputStream stream) throws IOException;
    }
}
