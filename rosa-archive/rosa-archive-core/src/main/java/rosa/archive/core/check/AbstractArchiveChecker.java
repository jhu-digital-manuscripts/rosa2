package rosa.archive.core.check;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.config.AppConfig;
import rosa.archive.core.serialize.Serializer;
import rosa.archive.model.ChecksumData;
import rosa.archive.model.ChecksumInfo;
import rosa.archive.model.HasId;
import rosa.archive.model.HashAlgorithm;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 */
public abstract class AbstractArchiveChecker <T extends HasId> {

    private AppConfig config;
    private Map<Class, Serializer> serializerMap;

    AbstractArchiveChecker(AppConfig config, Map<Class, Serializer> serializerMap) {
        this.config = config;
        this.serializerMap = serializerMap;
    }

    abstract boolean check(T t, ByteStreamGroup bsg, boolean checkBits);

    protected List<String> checkBits(ByteStreamGroup bsg) throws IOException {
        List<String> errors = new ArrayList<>();

        String checksumId = null;
        List<String> streams = bsg.listByteStreamNames();

        for (String name : streams) {
            if (name.contains(config.getSHA1SUM()) || name.contains(config.getBNF_MD5SUM())) {
                checksumId = name;
                break;
            }
        }

        errors.addAll(checkStreams(bsg, checksumId));

        return errors;
    }

    /**
     * Check the input streams in the specified ByteStreamGroup and all groups that it contains.
     * If a ByteStreamGroup contains stored checksum values for the other streams, the checksum
     * of each stream is calculated and compared to the stored value. Otherwise, the stream is
     * read. In either case, each stream is checked to see if it can be opened and read successfully.
     *
     * @param bsg byte stream group
     * @param checksumName name of checksum item in this group
     * @return list of errors found while performing check
     * @throws IOException
     */
    private List<String> checkStreams(ByteStreamGroup bsg, String checksumName) throws IOException {
        List<String> errors = new ArrayList<>();
        boolean compareChecksums = checksumName != null;

        ChecksumInfo checksums = null;
        if (compareChecksums) {
            try (InputStream in = bsg.getByteStream(checksumName)) {
                checksums = (ChecksumInfo) serializerMap.get(ChecksumInfo.class).read(in, errors);
            } catch (IOException e) {
                errors.add("Failed to read checksums. [" + checksumName + "]");
            }
        }

        for (String streamId : bsg.listByteStreamIds()) {
            try (InputStream in = bsg.getByteStream(streamId)){

                if (compareChecksums && checksums != null) {
                    ChecksumData cs = checksums.getChecksumDataForId(streamId);
                    String hash = calculateChecksum(in, cs.getAlgorithm());

                    if (!cs.getHash().equalsIgnoreCase(hash)) {
                        errors.add("Calculated hash value is different from stored value!\n"
                                        + "Calc: {" + streamId + ", " + hash + "}\n"
                                        + "Stored " + cs.toString()
                        );
                    }
                } else {
                    IOUtils.toByteArray(in);
                }

            } catch (IOException e) {
                errors.add("Could not read item. [" + streamId + "]");
            } catch (NoSuchAlgorithmException e) {
                errors.add("Failed to calculate checksum for [" + streamId + "]. Bad hashing algorithm.");
            }
        }

        for (ByteStreamGroup group : bsg.listByteStreamGroups()) {
            String csName = group.hasByteStream(group.name() + config.getSHA1SUM())
                    ? group.name() + config.getSHA1SUM() : null;
            errors.addAll(checkStreams(group, csName));
        }

        return errors;
    }

    /**
     * Compute the hash of an input stream using the specified algorithm.
     *
     * @param in input
     * @param algorithm hashing algorithm to use
     * @return hash value as hex string
     * @throws IOException
     * @throws java.security.NoSuchAlgorithmException
     */
    protected String calculateChecksum(InputStream in, HashAlgorithm algorithm)
            throws IOException, NoSuchAlgorithmException {
        MessageDigest md = DigestUtils.getDigest(algorithm.toString());
        DigestUtils.updateDigest(md, in);
        return Hex.encodeHexString(md.digest());
    }

}
