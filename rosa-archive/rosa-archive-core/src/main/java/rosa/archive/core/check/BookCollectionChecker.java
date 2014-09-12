package rosa.archive.core.check;

import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.config.AppConfig;
import rosa.archive.core.serialize.Serializer;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookScene;
import rosa.archive.model.CharacterNames;
import rosa.archive.model.HasId;
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTagging;
import rosa.archive.model.IllustrationTitles;
import rosa.archive.model.NarrativeSections;
import rosa.archive.model.NarrativeTagging;

import java.io.IOException;
import java.io.InputStream;
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
    public boolean checkContent(BookCollection collection, ByteStreamGroup bsg, boolean checkBits) {
        List<String> errors = new ArrayList<>();

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

        //   character_names and illustration_titles
        errors.addAll(check(collection.getCharacterNames(), collection.getIllustrationTitles(), bsg, collection));
        //   narrative_sections
        errors.addAll(check(collection.getNarrativeSections(), bsg, collection));
        //   missing

        // Check bit integrity (there is no stored checksum values for these files)

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
    private <T extends HasId> List<String> atteptToRead(T item, ByteStreamGroup bsg) {
        List<String> errors = new ArrayList<>();

        try (InputStream in = bsg.getByteStream(item.getId())) {
            // This will read the item in the archive and report any errors
            serializerMap.get(item.getClass()).read(in, errors);
        } catch (IOException e) {
            errors.add("Failed to read [" + item.getId() + "]");
        }

        return errors;
    }

    /**
     * Check books within a collection for references to character_names and illustration_titles.
     *
     * @param names character names to check
     * @param titles illustration titles to check
     * @param bsg byte stream group
     * @param collection parent collection
     * @return list of errors found while performing check
     */
    private List<String> check(CharacterNames names, IllustrationTitles titles,
                               ByteStreamGroup bsg, BookCollection collection) {
        List<String> errors = new ArrayList<>();

        if (names == null || !bsg.hasByteStream(names.getId())) {
            errors.add("character_names.csv missing from archive.");
            return errors;
        }

        // Make sure the character_names item can be read
        errors.addAll(atteptToRead(names, bsg));

        // Check character names references in image tagging in books
        List<String> booksInCollection = Arrays.asList(collection.books());
        try {
            for (String bookName : booksInCollection) {
                ByteStreamGroup b = bsg.getByteStreamGroup(bookName);
                if (b == null || !b.name().equals(bookName)) {
                    errors.add("Book missing from archive. [" + bookName + "]");
                    continue;
                }

                String id = bookName + config.getIMAGE_TAGGING();
                if (!b.hasByteStream(id)) {
                    errors.add("Image tagging missing from book. [" + bookName + "]");
                    continue;
                }

                Serializer serializer = serializerMap.get(IllustrationTagging.class);
                InputStream in = b.getByteStream(id);
                IllustrationTagging tagging = (IllustrationTagging) serializer.read(in, errors);

                for (Illustration ill : tagging) {
                    // Check character_names references in imagetag
                    List<String> characters = Arrays.asList(ill.getCharacters());
                    for (String character : characters) {
                        if (!names.hasCharacter(character)) {
                            errors.add("Character ID [" + character + "] in illustration [" +
                                    ill.getId() + "] missing.");
                        }
                    }

                    // Check illustration_titles references in imagetag
                    List<String> t = Arrays.asList(ill.getTitles());
                    for (String title : t) {
                        if (!titles.hasTitle(title)) {
                            errors.add("Title [" + title + "] in illustration [" + ill.getId() + "] missing.");
                        }
                    }
                }

            }
        } catch (IOException e) {
            errors.add("Failed to check book references to [" + names.getId() + "] or [" + titles.getId() + "]");
        }

        return errors;
    }

    /**
     *
     * @param sections item to check
     * @param bsg byte stream group
     * @return list of errors found while performing check
     */
    private List<String> check(NarrativeSections sections, ByteStreamGroup bsg, BookCollection collection) {
        List<String> errors = new ArrayList<>();

        if (sections == null || !bsg.hasByteStream(sections.getId())) {
            errors.add("narrative_sections.csv missing from archive.");
            return errors;
        }

        // Make sure narrative_sections can be read
        errors.addAll(atteptToRead(sections, bsg));

        // Check narrative sections references in narrative tagging in books
        List<String> booksInCollection = Arrays.asList(collection.books());
        try {
            for (String bookName : booksInCollection) {
                ByteStreamGroup b = bsg.getByteStreamGroup(bookName);
                if (b == null || !b.name().equals(bookName)) {
                    errors.add("Book missing from archive. [" + bookName + "]");
                    continue;
                }

                String id = bookName + config.getNARRATIVE_TAGGING();
                if (!b.hasByteStream(id)) {
                    errors.add("Narrative tagging missing from book. [" + bookName + "]");
                    continue;
                }

                Serializer serializer = serializerMap.get(NarrativeTagging.class);
                InputStream in = b.getByteStream(id);
                NarrativeTagging tagging = (NarrativeTagging) serializer.read(in, errors);

                for (BookScene scene : tagging) {

                }

            }
        } catch (IOException e) {
            errors.add("Failed to check book references to [" + sections.getId() + "]");
        }

        return errors;
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
