package rosa.iiif.presentation.core.transform.impl;

import rosa.iiif.presentation.model.IIIFNames;

public interface TransformerConstants extends IIIFNames {
    String IMAGE_RANGE_MISC_ID = "misc";
    String IMAGE_RANGE_BODYMATTER_ID = "bodymatter";
    String IMAGE_RANGE_BINDING_ID = "binding";
    String IMAGE_RANGE_ENDMATTER_ID = "endmatter";
    String IMAGE_RANGE_FRONTMATTER_ID = "frontmatter";
    String DEFAULT_SEQUENCE_LABEL = "reading-order";
    String PAGE_REGEX = "\\d{1,3}(r|v|R|V)";
    String TOP_RANGE_ID = "top";
    String ILLUSTRATION_RANGE_TYPE = "illus";
    String IMAGE_RANGE_TYPE = "image";
    String TEXT_RANGE_TYPE = "text";

    String KEY_CURRENT_LOC = "currentLocation";
    String KEY_REPO = "repository";
    String KEY_SHELFMARK = "shelfmark";
    String KEY_ORIGIN = "origin";
    String KEY_WIDTH = "width";
    String KEY_HEIGHT = "height";
    String KEY_YEAR_START = "yearStart";
    String KEY_YEAR_END = "yearEnd";
    String KEY_NUM_PAGES = "numberOfPages";
    String KEY_NUM_ILLS = "numberOfIllustrations";
    String KEY_TITLE = "title";
    String KEY_DATE = "date";
    String KEY_DIMS = "dimensions";
    String KEY_DIM_UNITS = "dimensionUnits";
    String KEY_TYPE = "type";
    String KEY_COMMON_NAME = "commonName";
    String KEY_MATERIAL = "material";
    String KEY_READER = "reader";
    String KEY_AUTHOR = "author";
}
