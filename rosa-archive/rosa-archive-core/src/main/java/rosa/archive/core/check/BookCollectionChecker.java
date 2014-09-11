package rosa.archive.core.check;

import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.config.AppConfig;
import rosa.archive.core.serialize.Serializer;
import rosa.archive.model.BookCollection;
import rosa.archive.model.HasId;

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
        errors.addAll(check(collection.getCharacterNames(), collection, bsg));
        //   illustration_names
        errors.addAll(check(collection.getIllustrationTitles(), collection, bsg));
        //   narrative_sections
        errors.addAll(check(collection.getNarrativeSections(), collection, bsg));
        //   missing

        // Check bit integrity
        errors.addAll(check(collection, bsg));

        return errors.isEmpty();
    }

    private <T extends HasId> List<String> check(
            T item, BookCollection collection, ByteStreamGroup bsg) {
        List<String> errors = new ArrayList<>();

        for (String book : collection.books()) {

            ByteStreamGroup bookStream = bsg.getByteStreamGroup(book);
            String bookName = bookStream.name();

            if (!bookStream.hasByteStream(item.getId())) {
                errors.add("Item missing from book [" + bookName + "] :: [" + item.getId() + "]");
                continue;
            }

            try {
                InputStream in = bookStream.getByteStream(item.getId());
                Object testItem = serializerMap.get(item.getClass()).read(in, errors);

                if (!item.equals(testItem)) {
                    errors.add("[" + item.getId() + "] different from item in book archive [" + bookName + "].");
                }

            } catch (IOException e) {
                errors.add("Failed to read item in archive. [" + item.getId() + "]");
            }
        }

        return errors;
    }

//    /**
//     * Check CharacterNames within a book collection.
//     *
//     * @param names item to check
//     * @param parent book collection containing the item to check
//     * @param bsg byte stream group with input streams
//     * @return list of errors found while performing check
//     */
//    private List<String> check(CharacterNames names, BookCollection parent, ByteStreamGroup bsg) {
//        List<String> errors = new ArrayList<>();
//
//        for (String book : parent.books()) {
//            ByteStreamGroup bookStream = bsg.getByteStreamGroup(book);
//            String bookId = bookStream.name();
//
//            String chNames = bookId + config.getCHARACTER_NAMES();
//            if (!bookStream.hasByteStream(chNames)) {
//                errors.add("Book does not have image tagging. [" + bookId + "]");
//                continue;
//            }
//
//            try {
//                InputStream in = bookStream.getByteStream(chNames);
//                CharacterNames testNames =
//                        (CharacterNames) serializerMap.get(CharacterNames.class).read(in, errors);
//
//                if (!names.equals(testNames)) {
//                    errors.add("Character names from book object [" + names.getId() + "] different" +
//                            " from character names item in archive [" + chNames + ".");
//                }
//            } catch (IOException e) {
//                errors.add("Could not read image tagging file. [" + chNames + "]");
//            }
//        }
//
//        return checkObjectInBooks(names, parent, bsg);
//    }
//
//    private List<String> check(IllustrationTitles titles, BookCollection parent, ByteStreamGroup bsg) {
//        List<String> errors = new ArrayList<>();
//
//        for (String book : parent.books()) {
//            ByteStreamGroup bookStream = bsg.getByteStreamGroup(book);
//            String bookId = bookStream.name();
//
//            String imTag = bookId + config.getIMAGE_TAGGING();
//            if (!bookStream.hasByteStream(imTag)) {
//                errors.add("Book does not have image tagging. [" + bookId + "]");
//                continue;
//            }
//
//            try {
//                InputStream in = bookStream.getByteStream(imTag);
//                IllustrationTitles testTitles =
//                        (IllustrationTitles) serializerMap.get(IllustrationTitles.class).read(in, errors);
//
//                if (!titles.equals(testTitles)) {
//                    errors.add("Character names from book object [" + titles.getId() + "] different" +
//                            " from character names item in archive [" + imTag + ".");
//                }
//            } catch (IOException e) {
//                errors.add("Could not read image tagging file. [" + imTag + "]");
//            }
//        }
//
//        return errors;
//        return checkObjectInBooks(titles, parent, bsg);
//    }
//
//    private List<String> check(NarrativeSections sections, BookCollection parent, ByteStreamGroup bsg) {
//        return new ArrayList<>();
//    }

    private List<String> check(BookCollection parent, ByteStreamGroup bsg) {
        List<String> errors = new ArrayList<>();

        // Try to read all InputStreams in this ByteStreamGroup
        try {
            List<String> streamIds = bsg.listByteStreamIds();

            for (String id : streamIds) {
                try (InputStream in = bsg.getByteStream(id)) {
                    List<String> lines = IOUtils.readLines(in);
                    if (lines == null || lines.size() == 0) {
                        errors.add("Did not read item. [" + id + "]");
                    }
                }
            }
        } catch (IOException e) {
            errors.add("Failed to read byte streams. [" + bsg + "]");
        }

        // For each ByteStreamGroup within [bsg], run this check
        try {
            for (ByteStreamGroup b : bsg.listByteStreamGroups()) {
                errors.addAll(check(parent, b));
            }
        } catch (IOException e) {
            errors.add("Failed to check byte stream group. [" + bsg + "]");
        }

        return errors;
    }
}
