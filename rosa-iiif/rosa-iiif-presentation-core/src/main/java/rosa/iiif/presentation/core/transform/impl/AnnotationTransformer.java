package rosa.iiif.presentation.core.transform.impl;

import com.google.inject.Inject;
import rosa.archive.core.ArchiveNameParser;
import rosa.archive.core.serialize.AORAnnotatedPageConstants;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.ImageList;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.iiif.presentation.core.IIIFRequestFormatter;
import rosa.iiif.presentation.core.transform.Transformer;
import rosa.iiif.presentation.model.annotation.Annotation;

import java.util.logging.Logger;

public class AnnotationTransformer extends BasePresentationTransformer implements Transformer<Annotation>,
        AORAnnotatedPageConstants {
    private static final String ANNOTATION_ILLUSTRATION = "illustration";
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

    /**
     * Get the archive annotation that was named.
     *
     * @param book archive book
     * @param annoName annotation name
     * @return the archive annotation named
     */
    private rosa.archive.model.aor.Annotation getArchiveAnnotation(Book book, String annoName) {
        AnnotatedPage annotatedPage = book.getAnnotationPage(getPage(annoName));

        int id = getPrimaryNumber(annoName);
        if (id < 0) {
            return null;
        }

        switch (getAnnotationType(annoName)) {
            case TAG_MARGINALIA:
                int[] ids = getMarginaliaNums(annoName);
                if (ids != null && ids.length == 3) {
                    annotatedPage.getMarginalia().get(ids[0])
                            .getLanguages().get(ids[1])
                            .getPositions().get(ids[2])
                            .getTexts();        // Transcription text here
                }
                break;
            case TAG_UNDERLINE:
                annotatedPage.getUnderlines().get(id);
                break;
            case TAG_SYMBOL:
                annotatedPage.getSymbols().get(id);
                break;
            case TAG_MARK:
                annotatedPage.getMarks().get(id);
                break;
            case TAG_NUMERAL:
                annotatedPage.getNumerals().get(id);
                break;
            case TAG_ERRATA:
                annotatedPage.getNumerals().get(id);
                break;
            case TAG_DRAWING:
                annotatedPage.getDrawings().get(id);
                break;
            case ANNOTATION_ILLUSTRATION:
                
                break;
            default:
                break;
        }

        return null;
    }

    /**
     * Get the page on which the annotation lives from the annotation name.
     *
     * @param name annotation name
     * @return the page associated with the annotation
     */
    private String getPage(String name) {
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
    private String getAnnotationType(String name) {
        return name.split("_")[1];
    }

    /**
     * Get the primary annotation ID number. Each annotation type will
     * have at least one identifying number associated with it.
     *
     * @param name annotation name
     * @return primary ID #
     */
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
     * <li>Which marginalia (also the marginalia primary ID #)</li>
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

    /**
     * For a given page name, find the associated BookImage
     *
     * @param images list of book images
     * @param page page name
     * @return the BookImage
     */
    private BookImage getPageImage(ImageList images, String page) {
        for (BookImage image : images) {
            if (image.getName().equals(page)) {
                return image;
            }
        }

        return null;
    }

}
