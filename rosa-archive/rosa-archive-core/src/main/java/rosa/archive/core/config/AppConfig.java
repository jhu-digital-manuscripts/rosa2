package rosa.archive.core.config;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 *
 */
public class AppConfig {

    @Inject @Named("ENCODING")
    public String CHARSET;

    @Inject @Named("TOP_COLLECTIONS_DIRECTORY")
    public String DIRECTORY;

    @Inject @Named("LANGUAGES")
    public String LANGUAGES;

    public String[] languages() {
        return LANGUAGES.split(",");
    }

    // Collection

    @Inject @Named("ILLUSTRATION_TITLES")
    public String ILLUSTRATION_TITLES;

    @Inject @Named("NARRATIVE_SECTIONS")
    public String NARRATIVE_SECTIONS;

    @Inject @Named("CHARACTER_NAMES")
    public String CHARACTER_NAMES;

    @Inject @Named("MISSING_IMAGE")
    public String MISSING_IMAGE;

    @Inject @Named("MISSING_PAGES")
    public String MISSING_PAGES;

    // Book

    @Inject @Named("CROP")
    public String CROP;

    @Inject @Named("IMAGES")
    public String IMAGES;

    @Inject @Named("IMAGES_CROP")
    public String IMAGES_CROP;

    @Inject @Named("IMAGE_TAGGING")
    public String IMAGE_TAGGING;

    @Inject @Named("NARRATIVE_TAGGING")
    public String NARRATIVE_TAGGING;

    @Inject @Named("NARRATIVE_TAGGING_MAN")
    public String NARRATIVE_TAGGING_MAN;

    @Inject @Named("REDUCED_TAGGING")
    public String REDUCED_TAGGING;

    @Inject @Named("SHA1SUM")
    public String SHA1SUM;

    @Inject @Named("TRANSCRIPTION")
    public String TRANSCRIPTION;

    @Inject @Named("DESCRIPTION")
    public String DESCRIPTION;

    @Inject @Named("PERMISSION")
    public String PERMISSION;

    @Inject @Named("BNF_FILEMAP")
    public String BNF_FILEMAP;

    @Inject @Named("BNF_FOLIATION")
    public String BNF_FOLIATION;

    @Inject @Named("BNF_MD5SUM")
    public String BNF_MD5SUM;

    // Images

    @Inject @Named("IMG_FRONTCOVER")
    public String IMG_FRONTCOVER;

    @Inject @Named("IMG_BACKCOVER")
    public String IMG_BACKCOVER;

    @Inject @Named("IMG_FRONTPASTEDOWN")
    public String IMG_FRONTPASTEDOWN;

    @Inject @Named("IMG_ENDPASTEDOWN")
    public String IMG_ENDPASTEDOWN;

    @Inject @Named("IMG_FRONT_FLYLEAF")
    public String IMG_FRONT_FLYLEAF;

    @Inject @Named("IMG_END_FLYLEAF")
    public String IMG_END_FLYLEAF;

    @Inject @Named("CROPPED_DIR")
    public String CROPPED_DIR;

    @Inject @Named("MISSING_PREFIX")
    public String MISSING_PREFIX;

    // Extensions

    @Inject @Named("CSV")
    public String CSV;

    @Inject @Named("XML")
    public String XML;

    @Inject @Named("TXT")
    public String TXT;

}
