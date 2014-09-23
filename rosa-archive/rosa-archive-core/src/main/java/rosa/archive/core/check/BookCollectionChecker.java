package rosa.archive.core.check;

import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.config.AppConfig;
import rosa.archive.core.serialize.Serializer;
import rosa.archive.model.BookCollection;
import rosa.archive.model.CharacterNames;
import rosa.archive.model.IllustrationTitles;
import rosa.archive.model.NarrativeSections;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class BookCollectionChecker extends AbstractArchiveChecker {

    @Inject
    public BookCollectionChecker(AppConfig config, Map<Class, Serializer> serializerMap) {
        super(config, serializerMap);
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
            errors.addAll(checkBits(bsg, false, false));
        }

        return errors.isEmpty();
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
}
