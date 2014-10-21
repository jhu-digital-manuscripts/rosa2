package rosa.archive.core.check;

import org.apache.commons.lang3.StringUtils;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.config.AppConfig;
import rosa.archive.core.serialize.Serializer;
import rosa.archive.core.util.ChecksumUtil;
import rosa.archive.model.SHA1Checksum;
import rosa.archive.model.HasId;
import rosa.archive.model.HashAlgorithm;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 */
public abstract class AbstractArchiveChecker {

    protected AppConfig config;
    protected Map<Class, Serializer> serializerMap;

    AbstractArchiveChecker(AppConfig config, Map<Class, Serializer> serializerMap) {
        this.config = config;
        this.serializerMap = serializerMap;
    }

    /**
     * Read an item from the archive, as identified by {@code item.getId()}. This ensures
     * that the object in the archive is readable. It does not check the bit integrity of
     * the object.
     *
     * @param item item to check
     * @param bsg byte stream group
     * @param <T> item type
     * @param errors list of errors
     * @param warnings list of warnings
     */
    protected <T extends HasId> void attemptToRead(T item, ByteStreamGroup bsg,
                                                   List<String> errors, List<String> warnings) {

        if (item == null || StringUtils.isBlank(item.getId())) {
            errors.add("Item missing from archive. [" + bsg.name() + "]");
            return;
        }

        try (InputStream in = bsg.getByteStream(item.getId())) {
            // This will read the item in the archive and report any errors
            serializerMap.get(item.getClass()).read(in, errors);
        } catch (IOException e) {
            errors.add("Failed to read [" + bsg.name() + ":" + item.getId() + "]");
        }
    }

    /**
     *
     * @param bsg byte stream group holding all streams to the archive
     * @param checkSubGroups TRUE - check streams in all sub-groups within {@code bsg}
     * @param required is the checksum validation required?
     * @param errors list of errors
     * @param warnings list of warnings
     */
    protected void checkBits(ByteStreamGroup bsg, boolean checkSubGroups, boolean required,
                                     List<String> errors, List<String> warnings) {
        String checksumId = null;
        List<String> streams = new ArrayList<>();
        try {
            // List all stream names, in order to look for CHECKSUM stream
            streams.addAll(bsg.listByteStreamNames());
        } catch (IOException e) {
            errors.add("Could not get byte stream names from group. [" + bsg.name() + "]");
        }

        // Look for CHECKSUM stream
        for (String name : streams) {
            if (name.contains(config.getSHA1SUM())) {
                checksumId = name;
                break;
            }
        }

        if (checksumId != null) {
            checkStreams(bsg, checksumId, errors, warnings);
        } else if (required) {
            // checksumId will always be NULL here (no checksum stream exists)
            errors.add("Failed to get checksum data from group. [" + bsg.name() + "]");
        }

        // Check all byte stream groups within 'bsg' if checkSubGroups is TRUE
        if (checkSubGroups) {
            List<ByteStreamGroup> subGroups = new ArrayList<>();
            try {
                subGroups.addAll(bsg.listByteStreamGroups());
            } catch (IOException e) {
                errors.add("Could not get byte stream groups from top level group. [" + bsg.name() + "]");
            }

            for (ByteStreamGroup group : subGroups) {
                checkBits(group, true, required, errors, warnings);
            }
        }
    }

    /**
     * Check the input streams in the specified ByteStreamGroup and all groups that it contains.
     * If a ByteStreamGroup contains stored checksum values for the other streams, the checksum
     * of each stream is calculated and compared to the stored value. Otherwise, the stream is
     * read. In either case, each stream is checked to see if it can be opened and read successfully.
     *
     * @param bsg byte stream group
     * @param checksumName name of checksum item in this group
     * @param errors list of errors
     * @param warnings list of warnings
     */
    protected List<String> checkStreams(ByteStreamGroup bsg, String checksumName,
                                        List<String> errors, List<String> warnings) {

        if (checksumName == null) {
            return errors;
        }

        // Load all stored checksum data
        SHA1Checksum SHA1Checksum = null;
        try (InputStream in = bsg.getByteStream(checksumName)) {
            SHA1Checksum = (SHA1Checksum) serializerMap.get(SHA1Checksum.class).read(in, errors);
        } catch (IOException e) {
            errors.add("Failed to read checksums. [" + bsg.name() + ":" + checksumName + "]");
        }

        if (SHA1Checksum == null) {
            return errors;
        }

        List<String> streamIds = new ArrayList<>();
        try {
            streamIds.addAll(bsg.listByteStreamNames());
        } catch (IOException e) {
            errors.add("Could not get stream IDs from group. [" + bsg.name() + "]");
        }

        // Calculate checksum for all InputStreams, compare to stored values
        for (String streamId : streamIds) {
            // Do not validate checksum for checksum file...
            if (streamId.equalsIgnoreCase(checksumName)) {
                continue;
            }

            String storedHash = SHA1Checksum.checksums().get(streamId);
            if (storedHash == null) {
                continue;
            }

            try (InputStream in = bsg.getByteStream(streamId)){
                String hash = ChecksumUtil.calculateChecksum(in, HashAlgorithm.SHA1);

                if (!storedHash.equalsIgnoreCase(hash)) {
                    errors.add("Calculated hash value is different from stored value!\n"
                                    + "    Calc:   {" + streamId + ", " + hash + "}\n"
                                    + "    Stored: {" + streamId + ", " + storedHash + "}"
                    );
                }

            } catch (IOException | NoSuchAlgorithmException e) {
                errors.add("Could not read item. [" + streamId + "]");
            }
        }

        return errors;
    }

}
