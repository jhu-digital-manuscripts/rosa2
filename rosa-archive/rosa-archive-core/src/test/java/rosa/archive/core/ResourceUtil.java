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
     * Recursively copy resource to the local file system.
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
     * Recursively copy resource to the local file system.
     * 
     * @param resource
     * @param dest
     * @throws IOException
     */
    public static void copyResource(URL resource, Path dest) throws IOException {
        String protocol = resource.getProtocol();

        if (protocol.equals("file")) {
            File src = FileUtils.toFile(resource);

            if (src.isDirectory()) {
                FileUtils.copyDirectory(src, dest.toFile());
            } else {
                FileUtils.copyFile(src, dest.toFile());
            }
        } else if (protocol.equals("jar")) {
            copyResource((JarURLConnection) resource.openConnection(), dest);
        } else {
            throw new IOException("Protocol unsupported: " + resource);
        }
    }

    private static void copyResource(JarURLConnection con, Path dest) throws IOException {
        JarFile jar = con.getJarFile();
        String prefix = con.getEntryName();

        for (JarEntry entry : Collections.list(jar.entries())) {
            String name = entry.getName();

            if (name.startsWith(prefix)) {
                Path path = dest.resolve(name.substring(prefix.length()));

                System.err.println(path + " -> " + path);

                if (entry.isDirectory()) {
                    Files.createDirectories(path);
                } else {
                    try (InputStream is = jar.getInputStream(entry)) {
                        Files.copy(is, dest);
                    }
                }
            }
        }
    }
}
