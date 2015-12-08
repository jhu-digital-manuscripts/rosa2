package rosa.iiif.presentation.core.util;

import rosa.archive.core.serialize.AORAnnotatedPageConstants;
import rosa.archive.model.Book;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Annotation;

import java.util.logging.Logger;

/**
 * Utility class for handling annotations.
 *
 */
public class Annotations implements AORAnnotatedPageConstants {
    private static final Logger logger = Logger.getLogger(Annotations.class.toString());
    private static final String ANNOTATION_ILLUSTRATION = "illustration";

    /**
     * Get the archive annotation that was named. TODO should test
     *
     * TODO Annotation ID structure should be defined externally
     * Annotation ID structure should be:
     *      page-id_annotation-type_number
     *
     * @param book archive book
     * @param annoName annotation name
     * @return the archive annotation named
     */
    public static Annotation getArchiveAnnotation(Book book, String annoName) {
        AnnotatedPage annotatedPage = book.getAnnotationPage(getPage(annoName));

        int id = getPrimaryNumber(annoName);
        if (id < 0) {
            logger.severe("Attempted to retrieve an annotation ID# less than 0. (" + annoName + ")");
            return null;
        }

        try {
            switch (getAnnotationType(annoName)) {
                case TAG_MARGINALIA:
                    return annotatedPage.getMarginalia().get(id);
                case TAG_UNDERLINE:
                    return annotatedPage.getUnderlines().get(id);
                case TAG_SYMBOL:
                    return annotatedPage.getSymbols().get(id);
                case TAG_MARK:
                    return annotatedPage.getMarks().get(id);
                case TAG_NUMERAL:
                    return annotatedPage.getNumerals().get(id);
                case TAG_ERRATA:
                    return annotatedPage.getNumerals().get(id);
                case TAG_DRAWING:
                    return annotatedPage.getDrawings().get(id);
                case ANNOTATION_ILLUSTRATION:
                default:
                    return null;
            }
        } catch (IndexOutOfBoundsException e) {
            logger.severe("Attempted to retrieve an annotation ID# was larger than the " +
                    "number of annotations. (" + annoName + ")");
            return null;
        }
    }

    /**
     * Get the page on which the annotation lives from the annotation name.
     *
     * @param name annotation name
     * @return the page associated with the annotation
     */
    private static String getPage(String name) {
        return name.split("_")[0];
    }

    /**
     * Get the annotation type from its name.
     *
     * <ul>
     * <li>marginalia</li>
     * <li>underline</li>
     * <li>mark</li>
     * <li>symbol</li>
     * <li>errata</li>
     * <li>numeral</li>
     * <li>drawing</li>
     * </ul>
     *
     * @param name annotation name
     * @return the type of annotation
     */
    private static String getAnnotationType(String name) {
        return name.split("_")[1];
    }

    /**
     * Get the primary annotation ID number. Each annotation type will
     * have at least one identifying number associated with it.
     *
     * @param name annotation name
     * @return primary ID #
     */
    private static int getPrimaryNumber(String name) {
        String num = name.split("_")[2];
        try {
            return Integer.parseInt(num);
        } catch (NumberFormatException e) {
            logger.warning("Failed to parse annotation primary id #. (" + name + ")");
            return -1;
        }
    }
}
