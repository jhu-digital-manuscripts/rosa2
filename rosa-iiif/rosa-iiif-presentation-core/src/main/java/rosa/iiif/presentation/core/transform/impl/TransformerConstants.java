package rosa.iiif.presentation.core.transform.impl;

import rosa.iiif.presentation.model.IIIFNames;

public interface TransformerConstants extends IIIFNames {
    static final String IMAGE_RANGE_MISC_ID = "misc";
    static final String IMAGE_RANGE_BODYMATTER_ID = "bodymatter";
    static final String IMAGE_RANGE_BINDING_ID = "binding";
    static final String IMAGE_RANGE_ENDMATTER_ID = "endmatter";
    static final String IMAGE_RANGE_FRONTMATTER_ID = "frontmatter";
    static final String DEFAULT_SEQUENCE_LABEL = "reading-order";
    static final String PAGE_REGEX = "\\d{1,3}(r|v|R|V)";
    static final String TOP_RANGE_ID = "top";
    static final String ILLUSTRATION_RANGE_TYPE = "illus";
    static final String IMAGE_RANGE_TYPE = "image";
    static final String TEXT_RANGE_TYPE = "text";
}
