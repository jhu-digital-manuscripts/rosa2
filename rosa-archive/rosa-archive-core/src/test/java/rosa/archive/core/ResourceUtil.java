package rosa.archive.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;

public class ResourceUtil {
    /**
     * Recursively copy resource to the local file system. The specified
     * resource will be placed inside dest.
     * 
     * @param klass
     * @param resource
     * @param dest
     * @throws IOException
     */
    public static void copyResource(Class<?> klass, String resource, Path dest) throws IOException {
        copyResource(klass.getResource(resource), dest);
    }

    /**
     * Recursively copy resource to the local file system. The specified
     * resource will be placed inside dest.
     * 
     * @param resource
     * @param dest
     *            directory
     * @throws IOException
     */
    public static void copyResource(URL resource, Path dest) throws IOException {
        String protocol = resource.getProtocol();

        if (protocol.equals("file")) {
            File src = FileUtils.toFile(resource);

            if (src.isDirectory()) {
                FileUtils.copyDirectoryToDirectory(src, dest.toFile());
            } else {
                FileUtils.copyFileToDirectory(src, dest.toFile());
            }
        } else if (protocol.equals("jar")) {
            copyResource((JarURLConnection) resource.openConnection(), dest);
        } else {
            throw new IOException("Protocol unsupported: " + resource);
        }
    }

    private static void copyResource(JarURLConnection con, Path dest) throws IOException {
        JarFile jar = con.getJarFile();

        // Only entries with this prefix are copied
        String prefix = con.getEntryName();

        for (JarEntry entry : Collections.list(jar.entries())) {
            String name = entry.getName();

            if (!name.startsWith(prefix)) {
                continue;
            }

            if (!entry.isDirectory()) {
                Path entry_dest = dest.resolve(name);

                Files.createDirectories(entry_dest.getParent());

                try (InputStream is = jar.getInputStream(entry)) {
                    Files.copy(is, entry_dest);
                }
            }
        }
    }
}
