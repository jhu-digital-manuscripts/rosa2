package rosa.archive.core.check;

import com.google.inject.Inject;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.config.AppConfig;
import rosa.archive.core.serialize.Serializer;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookScene;
import rosa.archive.model.CharacterNames;
import rosa.archive.model.ChecksumData;
import rosa.archive.model.ChecksumInfo;
import rosa.archive.model.HasId;
import rosa.archive.model.HashAlgorithm;
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTagging;
import rosa.archive.model.IllustrationTitles;
import rosa.archive.model.NarrativeSections;
import rosa.archive.model.NarrativeTagging;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class BookCollectionChecker {

    private AppConfig config;
    private Map<Class, Serializer> serializerMap;

    @Inject
    public BookCollectionChecker(AppConfig config, Map<Class, Serializer> serializerMap) {
        this.config = config;
        this.serializerMap = serializerMap;
    }

    public boolean checkContent(BookCollection collection, ByteStreamGroup bsg, boolean checkBits,
                                List<String> errors) {

        if (collection == null) {
            errors.add("Book collection missing.");
            return false;
        }

        // Check collection items:
        //   id
        if (StringUtils.isBlank(collection.getId())) {
            errors.add("Collection ID missing.");
        }

        //   books[]
        for (String id : collection.books()) {
            if (!bsg.hasByteStreamGroup(id)) {
                errors.add("Book ID in collection not found in archive. [" + id + "]");
            }
        }

        //   languages
        for (String lang : config.languages()) {
            if (!collection.isLanguageSupported(lang)) {
                errors.add("Language should be supported but is not. [" + lang + "]");
            }
        }

        //   character_names and illustration_titles and narrative_sections
        errors.addAll(check(collection, bsg));
        //   missing

        // Check bit integrity (there is no stored checksum values for these files)
        if (checkBits) {
            errors.addAll(checkBits(bsg));
        }

        return errors.isEmpty();
    }

    /**
     * Read an item from the archive, as identified by {@code item.getId()}. This ensures
     * that the object in the archive is readable. It does not check the bit integrity of
     * the object.
     *
     * @param item item to check
     * @param bsg byte stream group
     * @param <T> item type
     * @return list of errors found while performing the check
     */
    private <T extends HasId> List<String> attemptToRead(T item, ByteStreamGroup bsg) {
        List<String> errors = new ArrayList<>();

        if (item == null || StringUtils.isBlank(item.getId())) {
            errors.add("Item missing from archive.");
            return errors;
        }

        try (InputStream in = bsg.getByteStream(item.getId())) {
            // This will read the item in the archive and report any errors
            serializerMap.get(item.getClass()).read(in, errors);
        } catch (IOException e) {
            errors.add("Failed to read [" + item.getId() + "]");
        }

        return errors;
    }

    /**
     *
     *
     * @param bsg byte stream group
     * @param collection parent collection
     * @return list of errors found while performing check
     */
    private List<String> check(BookCollection collection, ByteStreamGroup bsg) {
        List<String> errors = new ArrayList<>();

        CharacterNames names = collection.getCharacterNames();
        IllustrationTitles titles = collection.getIllustrationTitles();
        NarrativeSections sections = collection.getNarrativeSections();

        // Make sure the things can be read
        errors.addAll(attemptToRead(names, bsg));
        errors.addAll(attemptToRead(titles, bsg));
        errors.addAll(attemptToRead(sections, bsg));

        return errors;
    }

    private List<String> checkBits(ByteStreamGroup bsg) {
        List<String> errors = new ArrayList<>();

        String guessChecksumName = bsg.name() + config.getSHA1SUM();
        ChecksumInfo checksumInfo = null;
        if (bsg.hasByteStream(guessChecksumName)) {
            try (InputStream checkIn = bsg.getByteStream(guessChecksumName)) {
                Serializer checksumSerializer = serializerMap.get(ChecksumInfo.class);
                checksumInfo = (ChecksumInfo) checksumSerializer.read(checkIn, errors);
            } catch (IOException e) {
                errors.add("Failed to load checksums. [" + guessChecksumName + "]");
            }
        }

        if (checksumInfo == null) {
            return errors;
        }

        List<String> streamIds = new ArrayList<>();
        try {
            streamIds.addAll(bsg.listByteStreamIds());
        } catch (IOException e) {
            errors.add("Failed to get stream IDs from group. [" + bsg.id() + "]");
        }

        for (String id : streamIds) {
            ChecksumData checksum = checksumInfo.getChecksumDataForId(id);

            if (checksum == null) {
                errors.add("No checksum data found for stream [" + id + "]");
                continue;
            }

            try (InputStream in = bsg.getByteStream(id)) {
                String hash = calculateChecksum(in, checksum.getAlgorithm());

                if (!checksum.getHash().equalsIgnoreCase(hash)) {
                    errors.add("Stream [" + id + "]: Stored checksum different from calculated checksum.");
                }
            } catch (IOException | NoSuchAlgorithmException e) {
                errors.add("Failed to read stream. [" + id + "]");
            }
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
