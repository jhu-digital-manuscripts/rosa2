package rosa.archive.core.util;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import rosa.archive.model.HashAlgorithm;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 */
public class ChecksumUtil {

    /**
     * Compute the hash of an input stream using the specified algorithm.
     *
     * @param in input
     * @param algorithm hashing algorithm to use
     * @return hash value as hex string
     *
     * @throws java.io.IOException input stream inaccessible
     * @throws java.security.NoSuchAlgorithmException hash algorithm not supported
     */
    public static String calculateChecksum(InputStream in, HashAlgorithm algorithm)
            throws IOException, NoSuchAlgorithmException {
        MessageDigest md = DigestUtils.getDigest(algorithm.toString());
        DigestUtils.updateDigest(md, in);
        return Hex.encodeHexString(md.digest());
    }

}
