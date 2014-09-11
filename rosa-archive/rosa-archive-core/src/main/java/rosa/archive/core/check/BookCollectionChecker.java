package rosa.archive.core.check;

import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.config.AppConfig;
import rosa.archive.core.serialize.Serializer;
import rosa.archive.model.BookCollection;
import rosa.archive.model.CharacterNames;
import rosa.archive.model.HasId;
import rosa.archive.model.IllustrationTitles;
import rosa.archive.model.NarrativeSections;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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

        //   character_names
        errors.addAll(check(collection.getCharacterNames(), bsg));
        //   illustration_names
        errors.addAll(check(collection.getIllustrationTitles(), bsg));
        //   narrative_sections
        errors.addAll(check(collection.getNarrativeSections(), bsg));
        //   missing

        // Check bit integrity

        return errors.isEmpty();
    }

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
     *
     * @param names item to check
     * @param bsg byte stream group
     * @return list of errors found while performing check
     */
    private List<String> check(CharacterNames names, ByteStreamGroup bsg) {
        List<String> errors = new ArrayList<>();

        if (names == null || !bsg.hasByteStream(names.getId())) {
            errors.add("character_names.csv missing from archive.");
            return errors;
        }

        // Make sure the character_names item can be read
        errors.addAll(atteptToRead(names, bsg));

        // Check character names references in image tagging in books

        return errors;
    }

    /**
     *
     * @param titles item to check
     * @param bsg byte stream group
     * @return list of errors found while performing check
     */
    private List<String> check(IllustrationTitles titles, ByteStreamGroup bsg) {
        List<String> errors = new ArrayList<>();

        if (titles == null || !bsg.hasByteStream(titles.getId())) {
            errors.add("illustraction_titles.csv missing from archive.");
            return errors;
        }

        // Make sure illustration_titles can be read
        errors.addAll(atteptToRead(titles, bsg));

        // Check illustration titles references in image tagging in books

        return errors;
    }

    /**
     *
     * @param sections item to check
     * @param bsg byte stream group
     * @return list of errors found while performing check
     */
    private List<String> check(NarrativeSections sections, ByteStreamGroup bsg) {
        List<String> errors = new ArrayList<>();

        if (sections == null || !bsg.hasByteStream(sections.getId())) {
            errors.add("narrative_sections.csv missing from archive.");
            return errors;
        }

        // Make sure narrative_sections can be read
        errors.addAll(atteptToRead(sections, bsg));

        // Check narrative sections references in narrative tagging in books

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
