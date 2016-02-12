package rosa.archive.core.check;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.serialize.SerializerSet;
import rosa.archive.model.BookCollection;

import com.google.inject.Inject;

/**
 *
 */
public class BookCollectionChecker extends AbstractArchiveChecker {

    /**
     * @param serializers all required serializers
     */
    @Inject
    public BookCollectionChecker(SerializerSet serializers) {
        super(serializers);
    }

    /**
     *
     * @param collection book collection to check
     * @param bsg byte stream group of the collection
     * @param checkBits validate the checksum values?
     * @param errors list of errors
     * @param warnings list of warnings
     * @return TRUE if the collection validates
     */
    public boolean checkContent(BookCollection collection, ByteStreamGroup bsg, boolean checkBits,
                                List<String> errors, List<String> warnings) {

        if (collection == null) {
            errors.add("Book collection missing.");
            return false;
        }

        // Check collection items:
        //   id
        if (StringUtils.isBlank(collection.getId())) {
            errors.add("Collection ID missing from collection. [" + collection.getId() + "]");
        }

        //   books[]
        for (String id : collection.books()) {
            if (!bsg.hasByteStreamGroup(id)) {
                errors.add("Book ID in collection not found in archive. [" + id + "]");
            }
        }

        //   languages
        for (String lang : collection.getAllSupportedLanguages()) {
            if (!collection.isLanguageSupported(lang)) {
                errors.add("Language should be supported but is not. [" + lang + "]");
            }
        }

//        if (!bsg.hasByteStream(MISSING_IMAGE)) {
//            errors.add("[" + MISSING_IMAGE + "] missing.");
//        }

        //   character_names and illustration_titles and narrative_sections
        check(collection, bsg, errors, warnings);

        // Checksum data
        if (collection.getChecksum() == null) {
            errors.add("Checksum file is missing for collection. [" + collection.getId() + "]");
        } else {
            // Check bit integrity (required)
            if (checkBits) {
                checkBits(bsg, false, true, errors, warnings);
            }
        }

        return errors.isEmpty();
    }

    /**
     * Two data sets available. For each set, if any single object is present,
     * they must all be present.
     *
     * Set 1: (character names, illustration titles, narrative sections)
     * Set 2: (books reference sheet, people reference sheet, locations reference sheet)
     *
     * @param bsg byte stream group
     * @param collection parent collection
     * @param errors list of errors
     * @param warnings list of warnings
     */
    private void check(BookCollection collection, ByteStreamGroup bsg, List<String> errors, List<String> warnings) {

        if (collection.getCharacterNames() != null || collection.getIllustrationTitles() != null
                || collection.getNarrativeSections() != null) {
            // Make sure the things can be read
            attemptToRead(collection.getCharacterNames(), bsg, errors, warnings);
            attemptToRead(collection.getIllustrationTitles(), bsg, errors, warnings);
            attemptToRead(collection.getNarrativeSections(), bsg, errors, warnings);
        }

        if (collection.getBooksRef() != null || collection.getPeopleRef() != null
                || collection.getLocationsRef() != null) {
            // If any AoR data sheet is present, they all must be present
            attemptToRead(collection.getBooksRef(), bsg, errors, warnings);
            attemptToRead(collection.getPeopleRef(), bsg, errors, warnings);
            attemptToRead(collection.getLocationsRef(), bsg, errors, warnings);
        }
    }
}
