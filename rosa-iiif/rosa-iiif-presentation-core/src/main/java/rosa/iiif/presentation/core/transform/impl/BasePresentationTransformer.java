package rosa.iiif.presentation.core.transform.impl;

import rosa.archive.core.ArchiveNameParser;
import rosa.iiif.presentation.core.IIIFRequestFormatter;
import rosa.iiif.presentation.model.IIIFNames;
import rosa.iiif.presentation.model.PresentationRequest;
import rosa.iiif.presentation.model.PresentationRequestType;

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
    protected ArchiveNameParser nameParser;

    public BasePresentationTransformer(IIIFRequestFormatter presRequestFormatter,
                                       ArchiveNameParser nameParser) {
        this.presRequestFormatter = presRequestFormatter;
        this.nameParser = nameParser;
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
