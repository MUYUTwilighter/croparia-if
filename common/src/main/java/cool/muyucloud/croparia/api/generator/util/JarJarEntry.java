package cool.muyucloud.croparia.api.generator.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarJarEntry {
    private final File jarFile;
    private final JarEntry entry;
    private final InputStreamSupplier supplier;

    public JarJarEntry(File jarFile, JarEntry entry) {
        this(jarFile, entry, () -> {
            JarFile jar = new JarFile(jarFile);
            InputStream stream = jar.getInputStream(entry);
            return new JarBackedInputStream(jar, stream);
        });
    }

    public JarJarEntry(File jarFile, JarEntry entry, InputStreamSupplier supplier) {
        this.jarFile = jarFile;
        this.entry = entry;
        this.supplier = supplier;
    }

    public static JarJarEntry ofFile(File root, File file) {
        String entryName = root.toPath().relativize(file.toPath()).toString().replace('\\', '/');
        return new JarJarEntry(root, new JarEntry(entryName), () -> java.nio.file.Files.newInputStream(file.toPath()));
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
        try (InputStream stream = this.supplier.open()) {
            consumer.accept(stream);
        }
    }

    public interface InputStreamConsumer {
        void accept(InputStream stream) throws IOException;
    }

    @FunctionalInterface
    public interface InputStreamSupplier {
        InputStream open() throws IOException;
    }

    private static final class JarBackedInputStream extends InputStream {
        private final JarFile jar;
        private final InputStream delegate;

        private JarBackedInputStream(JarFile jar, InputStream delegate) {
            this.jar = jar;
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException {
            return this.delegate.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            return this.delegate.read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return this.delegate.read(b, off, len);
        }

        @Override
        public byte[] readAllBytes() throws IOException {
            return this.delegate.readAllBytes();
        }

        @Override
        public long transferTo(java.io.OutputStream out) throws IOException {
            return this.delegate.transferTo(out);
        }

        @Override
        public void close() throws IOException {
            try {
                this.delegate.close();
            } finally {
                this.jar.close();
            }
        }
    }
}
