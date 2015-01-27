package rosa.iiif.presentation.core.transform;

import rosa.archive.core.ArchiveNameParser;
import rosa.archive.model.Book;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookMetadata;
import rosa.iiif.presentation.core.IIIFRequestFormatter;
import rosa.iiif.presentation.core.ImageIdMapper;
import rosa.iiif.presentation.model.*;

import java.util.HashMap;
import java.util.Map;

public abstract class BasePresentationTransformer implements IIIFNames {
    protected static final String IMAGE_RANGE_MISC_ID = "misc";
    protected static final String IMAGE_RANGE_BODYMATTER_ID = "bodymatter";
    protected static final String IMAGE_RANGE_BINDING_ID = "binding";
    protected static final String IMAGE_RANGE_ENDMATTER_ID = "endmatter";
    protected static final String IMAGE_RANGE_FRONTMATTER_ID = "frontmatter";
    protected static final String DEFAULT_SEQUENCE_LABEL = "reading-order";
    protected static final String PAGE_REGEX = "\\d{1,3}(r|v|R|V)";
    protected static final String TOP_RANGE_ID = "top";
    protected static final String ILLUSTRATION_RANGE_TYPE = "illus";
    protected static final String IMAGE_RANGE_TYPE = "image";
    protected static final String TEXT_RANGE_TYPE = "text";

    protected IIIFRequestFormatter presRequestFormatter;
    protected rosa.iiif.image.core.IIIFRequestFormatter imageRequestFormatter;
    protected ArchiveNameParser nameParser;

    public BasePresentationTransformer(IIIFRequestFormatter presRequestFormatter,
                                       rosa.iiif.image.core.IIIFRequestFormatter imageRequestFormatter,
                                       ArchiveNameParser nameParser) {
        this.presRequestFormatter = presRequestFormatter;
        this.imageRequestFormatter = imageRequestFormatter;
        this.nameParser = nameParser;
    }

    /**
     * Handle the book's structured metadata and manifest it into Manifest metadata.
     *
     * @param book book
     * @param languages languages available
     */
    protected Map<String, HtmlValue> transformMetadata(Book book, String[] languages) {
        Map<String, HtmlValue> map = new HashMap<>();

        for (String lang : languages) {
            BookMetadata metadata = book.getBookMetadata(lang);

            map.put("currentLocation", new HtmlValue(metadata.getCurrentLocation(), lang));
            map.put("repository", new HtmlValue(metadata.getRepository(), lang));
            map.put("shelfmark", new HtmlValue(metadata.getShelfmark(), lang));
            map.put("origin", new HtmlValue(metadata.getOrigin(), lang));

            if (metadata.getWidth() != -1) {
                map.put("width", new HtmlValue(metadata.getWidth() + "", lang));
            }
            if (metadata.getHeight() != -1) {
                map.put("height", new HtmlValue(metadata.getHeight() + "", lang));
            }
            if (metadata.getYearStart() != -1) {
                map.put("yearStart", new HtmlValue(metadata.getYearStart() + "", lang));
            }
            if (metadata.getYearEnd() != -1) {
                map.put("yearEnd", new HtmlValue(metadata.getYearEnd() + "", lang));
            }
            if (metadata.getNumberOfPages() != -1) {
                map.put("numberOfPages", new HtmlValue(metadata.getNumberOfPages() + "", lang));
            }
            if (metadata.getNumberOfIllustrations() != -1) {
                map.put("numberOfIllustrations", new HtmlValue(metadata.getNumberOfIllustrations() + "", lang));
            }
            if (metadata.getTitle() != null) {
                map.put("title", new HtmlValue(metadata.getTitle(), lang));
            }
            if (metadata.getDate() != null) {
                map.put("date", new HtmlValue(metadata.getDate(), lang));
            }
            if (metadata.getDimensions() != null) {
                map.put("dimensions", new HtmlValue(metadata.getDimensions(), lang));
            }
            if (metadata.getDimensionUnits() != null) {
                map.put("dimensionUnits", new HtmlValue(metadata.getDimensionUnits(), lang));
            }
            if (metadata.getType() != null) {
                map.put("type", new HtmlValue(metadata.getType(), lang));
            }
            if (metadata.getCommonName() != null) {
                map.put("commonName", new HtmlValue(metadata.getCommonName(), lang));
            }
            if (metadata.getMaterial() != null) {
                map.put("material", new HtmlValue(metadata.getMaterial(), lang));
            }

            // TODO book texts
        }

        return map;
    }

    protected String urlId(String collection, String book, String name, PresentationRequestType type) {
        return presRequestFormatter.format(presentationRequest(collection, book, name, type));
    }

    private String presentationId(String collection, String book) {
        return collection + (book == null || book.isEmpty() ? "" : "." + book);
    }

    private PresentationRequest presentationRequest(String collection, String book, String name,
                                                    PresentationRequestType type) {
        return new PresentationRequest(presentationId(collection, book), name, type);
    }



}
