package rosa.iiif.presentation.core.transform.impl;

import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.core.PresentationUris;
import rosa.iiif.presentation.model.IIIFNames;

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
    protected static final String TRANSCRIPTION_ID_LABEL = "Transcription ID";
    protected static final String IMAGE_ID_LABEL = "Image ID";

    protected final PresentationUris pres_uris;

    public BasePresentationTransformer(IIIFPresentationRequestFormatter presRequestFormatter) {
        this.pres_uris = new PresentationUris(presRequestFormatter);
    }
}
