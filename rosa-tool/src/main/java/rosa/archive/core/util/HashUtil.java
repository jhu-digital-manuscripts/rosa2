package rosa.archive.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility class for computing cryptographic hashes of files.
 */
public final class HashUtil {

    private static final String SHA1_ALGORITHM = "SHA-1";
    private static final int BUFFER_SIZE = 8192;

    private HashUtil() {
        // Utility class, not instantiable
    }

    /**
     * Computes the SHA-1 hash of a file and returns it as a lowercase hex string.
     *
     * @param file the path to the file to hash
     * @return the SHA-1 hash as a lowercase hexadecimal string
     * @throws IOException if the file cannot be read
     */
    public static String computeSHA1(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA1_ALGORITHM);
            byte[] buffer = new byte[BUFFER_SIZE];

            try (InputStream in = Files.newInputStream(file)) {
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-1 algorithm not available", e);
        }
    }
}
