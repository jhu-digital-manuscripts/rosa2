package rosa.iiif.presentation.core.transform.impl;

import com.google.inject.Inject;
import rosa.archive.core.ArchiveNameParser;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.iiif.presentation.core.IIIFRequestFormatter;
import rosa.iiif.presentation.core.transform.Transformer;
import rosa.iiif.presentation.model.annotation.Annotation;

import java.util.logging.Logger;

public class AnnotationTransformer extends BasePresentationTransformer implements Transformer<Annotation> {
    private static final Logger logger = Logger.getLogger(AnnotationTransformer.class.toString());

    private ArchiveNameParser imageNameParser;

    @Inject
    public AnnotationTransformer(IIIFRequestFormatter presRequestFormatter, ArchiveNameParser imageNameParser) {
        super(presRequestFormatter);
        this.imageNameParser = imageNameParser;
    }

    @Override
    public Annotation transform(BookCollection collection, Book book, String name) {
        // Find annotation in book


        // Transform archive anno -> iiif anno
        return null;
    }

    @Override
    public Class<Annotation> getType() {
        return Annotation.class;
    }

    private String getPage(String name) {
        return name.split("_")[0];
    }

    private String getAnnotationType(String name) {
        return name.split("_")[1];
    }

    private int getPrimaryNumber(String name) {
        String num = name.split("_")[2];
        try {
            return Integer.parseInt(num);
        } catch (NumberFormatException e) {
            logger.warning("Failed to parse annotation primary id #. (" + name + ")");
            return -1;
        }
    }

    /**
     * Marginalia text transcriptions require more information to identify location
     * than other annotations. {@link #getPrimaryNumber(String)}
     *
     * In the AoR transcriptions, lines of transcriptions lie along the path:
     * {@code (marginalia base)/language/position/text}. Thus, to identify a particular
     * line of transcription, one must know:
     *
     * <ol>
     * <li>Which marginalia</li>
     * <li>Which language within the marginalia</li>
     * <li>Which position within the language</li>
     * </ol>
     *
     * These three items will be integer numbers representing the line's indexes within
     * various lists.
     *
     * @param name annotation name
     * @return array containing THREE marginalia ID nums, or NULL if annotation name
     *          is not for a marginalia
     */
    private int[] getMarginaliaNums(String name) {
        // <page_id>_marginalia_primaryNum_languageNum_positionNum
        String[] parts = name.split("_");

        try {
            return new int[]{
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]),
                    Integer.parseInt(parts[4])
            };
        } catch (NumberFormatException e) {
            logger.warning("Failed to parse a marginalia ID number. (" + name + ")");
        } catch (IndexOutOfBoundsException e) {
            logger.warning("Not enough parts in name to represent a marginalia annotation. (" + name + ")");
        }

        return null;
    }

}
