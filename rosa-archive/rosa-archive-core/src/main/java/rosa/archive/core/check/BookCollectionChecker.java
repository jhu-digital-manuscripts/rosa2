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
public class BookCollectionChecker implements Checker<BookCollection> {

    private AppConfig config;
    private Map<Class, Serializer> serializerMap;

    @Inject
    public BookCollectionChecker(AppConfig config, Map<Class, Serializer> serializerMap) {
        this.config = config;
        this.serializerMap = serializerMap;
    }

    @Override
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
     * Check books within a collection for references to character_names and illustration_titles
     * and narrative_sections.
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

        // Check character names references in image tagging in books
        List<String> booksInCollection = Arrays.asList(collection.books());
        try {
            for (String bookName : booksInCollection) {
                ByteStreamGroup bookBSG = bsg.getByteStreamGroup(bookName);
                // Make sure that a book in the collection's book list exists in the archive
                if (bookBSG == null || !bookBSG.name().equals(bookName)) {
                    errors.add("Book missing from archive. [" + bookName + "]");
                    continue;
                }

                // Check character_names and illustration_titles
                errors.addAll(check(names, titles, bookBSG));
                // Check narrative_sections (automatic and manual)
                errors.addAll(check(sections, bookBSG));

            }
        } catch (IOException e) {
            errors.add("Failed to check book references to [" + names.getId() + "] or [" + titles.getId() + "]"
                    + " or [" + sections.getId() + "]");
        }

        return errors;
    }

    /**
     *
     * @param names character names
     * @param titles illustration titles
     * @param bsg byte stream group
     * @return list of errors found while checking
     * @throws IOException
     */
    private List<String> check(CharacterNames names, IllustrationTitles titles, ByteStreamGroup bsg)
            throws IOException {
        List<String> errors = new ArrayList<>();

        String id = bsg.name() + config.getIMAGE_TAGGING();
        if (!bsg.hasByteStream(id)) {
            // Return nothing if there is no image tagging to check
            return errors;
        }

        Serializer serializer = serializerMap.get(IllustrationTagging.class);
        InputStream in = bsg.getByteStream(id);
        IllustrationTagging tagging = (IllustrationTagging) serializer.read(in, errors);

        // Checking CharacterNames and IllustrationTitles, referenced in image tagging
        for (Illustration ill : tagging) {
            // Check character_names references in imagetag
            if (names != null) {
                List<String> characters = Arrays.asList(ill.getCharacters());
                for (String character : characters) {
                    for (String charId : numericIds(character)) {
                        if (!names.hasCharacter(charId)) {
                            errors.add("Character ID [" + charId + "] in illustration [" +
                                    ill.getId() + "] missing.");
                        }
                    }

                }
            }

            // Check illustration_titles references in imagetag
            if (titles != null) {
                List<String> t = Arrays.asList(ill.getTitles());
                for (String title : t) {
                    for (String titleId : numericIds(title)) {
                        if (!titles.hasTitle(titleId)) {
                            errors.add("Title [" + titleId + "] in illustration [" + ill.getId() + "] missing.");
                        }
                    }

                }
            }
        }

        return errors;
    }

    /**
     * Get only numerical IDs from a comma delimited string.
     *
     * @param ref string possibly containing reference IDs
     * @return array with only numerical IDs
     */
    protected String[] numericIds(String ref) {
        String[] rawParts = ref.split(",");

        // Leave text out.
        List<String> onlyNumbers = new ArrayList<>();
        for (String rawPart : rawParts) {
            if (rawPart.matches("\\d+")) {
                onlyNumbers.add(rawPart);
            }
        }

        return onlyNumbers.toArray(new String[onlyNumbers.size()]);
    }

    /**
     *
     * @param sections item to check
     * @param bookBSG byte stream group
     * @return list of errors found while performing check
     */
    private List<String> check(NarrativeSections sections, ByteStreamGroup bookBSG) throws IOException {
        List<String> errors = new ArrayList<>();

        Serializer serializer = serializerMap.get(NarrativeTagging.class);

        // Check narrative sections references in narrative tagging in books
        // both automatic and narrative tagging, if they exist
        String autoId = bookBSG.name() + config.getNARRATIVE_TAGGING();
        if (bookBSG.hasByteStream(autoId)) {
            InputStream autoIn = bookBSG.getByteStream(autoId);
            check(sections, (NarrativeTagging) serializer.read(autoIn, errors));
        }

        String manId = bookBSG.name() + config.getNARRATIVE_TAGGING_MAN();
        if (bookBSG.hasByteStream(manId)) {
            InputStream manIn = bookBSG.getByteStream(manId);
            check(sections, (NarrativeTagging) serializer.read(manIn, errors));
        }

        return errors;
    }

    /**
     * Make sure the IDs in a book's narrative tagging exist in the collections narrative sections.
     *
     * @param sections narrative sections to check
     * @param tagging image tagging to check against
     * @return list of errors found while checking
     */
    private List<String> check(NarrativeSections sections, NarrativeTagging tagging) {
        List<String> errors = new ArrayList<>();

        for (BookScene scene : tagging) {
            if (sections.findIndexOfSceneById(scene.getId()) < 0) {
                errors.add("Narrative tagging scene not found in narrative_sections.");
            }
        }

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

//    /**
//     *
//     * @param bsg byte stream group
//     * @return list of errors found while performing check
//     */
//    private List<String> check(ByteStreamGroup bsg) {
//        List<String> errors = new ArrayList<>();
//
//        // Try to read all InputStreams in this ByteStreamGroup
//        try {
//            List<String> streamIds = bsg.listByteStreamIds();
//
//            for (String id : streamIds) {
//                try (InputStream in = bsg.getByteStream(id)) {
//                    List<String> lines = IOUtils.readLines(in);
//                    if (lines == null || lines.size() == 0) {
//                        errors.add("Did not read item. [" + id + "]");
//                    }
//                }
//            }
//        } catch (IOException e) {
//            errors.add("Failed to read byte streams. [" + bsg + "]");
//        }
//
//        // For each ByteStreamGroup within [bsg], run this check
//        try {
//            for (ByteStreamGroup b : bsg.listByteStreamGroups()) {
//                errors.addAll(check(b));
//            }
//        } catch (IOException e) {
//            errors.add("Failed to check byte stream group. [" + bsg + "]");
//        }
//
//        return errors;
//    }
}
