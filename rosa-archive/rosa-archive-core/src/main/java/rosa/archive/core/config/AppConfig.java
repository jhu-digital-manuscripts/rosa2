package rosa.archive.core.config;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 *
 */
public class AppConfig {

    @Inject @Named("ENCODING")
    private String CHARSET;

    @Inject @Named("TOP_COLLECTIONS_DIRECTORY")
    private String DIRECTORY;

    @Inject @Named("LANGUAGES")
    private String LANGUAGES;

    // Collection

    @Inject @Named("ILLUSTRATION_TITLES")
    private String ILLUSTRATION_TITLES;

    @Inject @Named("NARRATIVE_SECTIONS")
    private String NARRATIVE_SECTIONS;

    @Inject @Named("CHARACTER_NAMES")
    private String CHARACTER_NAMES;

    @Inject @Named("MISSING_IMAGE")
    private String MISSING_IMAGE;

    @Inject @Named("MISSING_PAGES")
    private String MISSING_PAGES;

    // Book

    @Inject @Named("CROP")
    private String CROP;

    @Inject @Named("IMAGES")
    private String IMAGES;

    @Inject @Named("IMAGES_CROP")
    private String IMAGES_CROP;

    @Inject @Named("IMAGE_TAGGING")
    private String IMAGE_TAGGING;

    @Inject @Named("NARRATIVE_TAGGING")
    private String NARRATIVE_TAGGING;

    @Inject @Named("NARRATIVE_TAGGING_MAN")
    private String NARRATIVE_TAGGING_MAN;

    @Inject @Named("REDUCED_TAGGING")
    private String REDUCED_TAGGING;

    @Inject @Named("SHA1SUM")
    private String SHA1SUM;

    @Inject @Named("TRANSCRIPTION")
    private String TRANSCRIPTION;

    @Inject @Named("DESCRIPTION")
    private String DESCRIPTION;

    @Inject @Named("PERMISSION")
    private String PERMISSION;

    @Inject @Named("BNF_FILEMAP")
    private String BNF_FILEMAP;

    @Inject @Named("BNF_FOLIATION")
    private String BNF_FOLIATION;

    @Inject @Named("BNF_MD5SUM")
    private String BNF_MD5SUM;

    // Images

    @Inject @Named("IMG_FRONTCOVER")
    private String IMG_FRONTCOVER;

    @Inject @Named("IMG_BACKCOVER")
    private String IMG_BACKCOVER;

    @Inject @Named("IMG_FRONTPASTEDOWN")
    private String IMG_FRONTPASTEDOWN;

    @Inject @Named("IMG_ENDPASTEDOWN")
    private String IMG_ENDPASTEDOWN;

    @Inject @Named("IMG_FRONT_FLYLEAF")
    private String IMG_FRONT_FLYLEAF;

    @Inject @Named("IMG_END_FLYLEAF")
    private String IMG_END_FLYLEAF;

    @Inject @Named("CROPPED_DIR")
    private String CROPPED_DIR;

    @Inject @Named("MISSING_PREFIX")
    private String MISSING_PREFIX;

    // Extensions

    @Inject @Named("CSV")
    private String CSV;

    @Inject @Named("XML")
    private String XML;

    @Inject @Named("TXT")
    private String TXT;

    @Inject @Named("TIF")
    private String TIF;

    // -----------------------------------------------------------------------------------------
    // ---------- Metadata XML -----------------------------------------------------------------
    // -----------------------------------------------------------------------------------------

    @Inject @Named("metadata.xml.date")
    private String metadataDateTag;

    @Inject @Named("metadata.xml.currentLocation")
    private String metadataCurrentLocationTag;

    @Inject @Named("metadata.xml.repository")
    private String metadataRepositoryTag;

    @Inject @Named("metadata.xml.shelfmark")
    private String metadataShelfmarkTag;

    @Inject @Named("metadata.xml.origin")
    private String metadataOriginTag;

    @Inject @Named("metadata.xml.width")
    private String metadataWidthTag;

    @Inject @Named("metadata.xml.height")
    private String metadataHeightTag;

    @Inject @Named("metadata.xml.numberOfIllustrations")
    private String metadataNumIllustrationsTag;

    @Inject @Named("metadata.xml.commonName")
    private String metadataCommonNameTag;

    @Inject @Named("metadata.xml.material")
    private String metadataMaterialTag;

    @Inject @Named("metadata.xml.type")
    private String metadataTypeTag;

    @Inject @Named("metadata.xml.measure")
    private String metadataMeasureTag;

    @Inject @Named("metadata.xml.numberOfPages")
    private String metadataNumPagesTag;

    @Inject @Named("metadata.xml.yearStart")
    private String metadataYearStartTag;

    @Inject @Named("metadata.xml.yearEnd")
    private String metadataYearEndTag;

    @Inject @Named("metadata.xml.texts")
    private String metadataTextsTag;

    @Inject @Named("metadata.xml.texts.linesPerColumn")
    private String metadataTextsLinesPerColTag;

    @Inject @Named("metadata.xml.texts.columnsPerPage")
    private String metadataTextsColsPerPageTag;

    @Inject @Named("metadata.xml.texts.leavesPerGathering")
    private String metadataTextsLeavesPerGatheringTag;

    @Inject @Named("metadata.xml.texts.numberOfPages")
    private String metadataTextsNumPagesTag;

    @Inject @Named("metadata.xml.texts.id")
    private String metadataTextsIdTag;

    @Inject @Named("metadata.xml.texts.title")
    private String metadataTextsTitleTag;

    @Inject @Named("metadata.xml.texts.locus")
    private String metadataTextsLocusTag;

    @Inject @Named("metadata.xml.texts.firstPage")
    private String metadataTextsFirstPageTag;

    @Inject @Named("metadata.xml.texts.lastPage")
    private String metadataTextsLastPageTag;

    @Inject @Named("annotation.schema.url")
    private String annotationSchemaUrl;

    @Inject @Named("annotation.dtd.url")
    private String annotationDtdUrl;

    // -----------------------------------------------------------------------------------------
    // ---------- getters ----------------------------------------------------------------------
    // -----------------------------------------------------------------------------------------

    public String[] languages() {
        return LANGUAGES.split(",");
    }

    public String getCHARSET() {
        return CHARSET;
    }

    public String getDIRECTORY() {
        return DIRECTORY;
    }

    public String getLANGUAGES() {
        return LANGUAGES;
    }

    public String getILLUSTRATION_TITLES() {
        return ILLUSTRATION_TITLES;
    }

    public String getNARRATIVE_SECTIONS() {
        return NARRATIVE_SECTIONS;
    }

    public String getCHARACTER_NAMES() {
        return CHARACTER_NAMES;
    }

    public String getMISSING_IMAGE() {
        return MISSING_IMAGE;
    }

    public String getMISSING_PAGES() {
        return MISSING_PAGES;
    }

    public String getCROP() {
        return CROP;
    }

    public String getIMAGES() {
        return IMAGES;
    }

    public String getIMAGES_CROP() {
        return IMAGES_CROP;
    }

    public String getIMAGE_TAGGING() {
        return IMAGE_TAGGING;
    }

    public String getNARRATIVE_TAGGING() {
        return NARRATIVE_TAGGING;
    }

    public String getNARRATIVE_TAGGING_MAN() {
        return NARRATIVE_TAGGING_MAN;
    }

    public String getREDUCED_TAGGING() {
        return REDUCED_TAGGING;
    }

    public String getSHA1SUM() {
        return SHA1SUM;
    }

    public String getTRANSCRIPTION() {
        return TRANSCRIPTION;
    }

    public String getDESCRIPTION() {
        return DESCRIPTION;
    }

    public String getPERMISSION() {
        return PERMISSION;
    }

    public String getBNF_FILEMAP() {
        return BNF_FILEMAP;
    }

    public String getBNF_FOLIATION() {
        return BNF_FOLIATION;
    }

    public String getBNF_MD5SUM() {
        return BNF_MD5SUM;
    }

    public String getIMG_FRONTCOVER() {
        return IMG_FRONTCOVER;
    }

    public String getIMG_BACKCOVER() {
        return IMG_BACKCOVER;
    }

    public String getIMG_FRONTPASTEDOWN() {
        return IMG_FRONTPASTEDOWN;
    }

    public String getIMG_ENDPASTEDOWN() {
        return IMG_ENDPASTEDOWN;
    }

    public String getIMG_FRONT_FLYLEAF() {
        return IMG_FRONT_FLYLEAF;
    }

    public String getIMG_END_FLYLEAF() {
        return IMG_END_FLYLEAF;
    }

    public String getCROPPED_DIR() {
        return CROPPED_DIR;
    }

    public String getMISSING_PREFIX() {
        return MISSING_PREFIX;
    }

    public String getCSV() {
        return CSV;
    }

    public String getXML() {
        return XML;
    }

    public String getTXT() {
        return TXT;
    }

    public String getTIF() {
        return TIF;
    }

    public String getMetadataDateTag() {
        return metadataDateTag;
    }

    public String getMetadataCurrentLocationTag() {
        return metadataCurrentLocationTag;
    }

    public String getMetadataRepositoryTag() {
        return metadataRepositoryTag;
    }

    public String getMetadataShelfmarkTag() {
        return metadataShelfmarkTag;
    }

    public String getMetadataOriginTag() {
        return metadataOriginTag;
    }

    public String getMetadataWidthTag() {
        return metadataWidthTag;
    }

    public String getMetadataHeightTag() {
        return metadataHeightTag;
    }

    public String getMetadataNumIllustrationsTag() {
        return metadataNumIllustrationsTag;
    }

    public String getMetadataCommonNameTag() {
        return metadataCommonNameTag;
    }

    public String getMetadataMaterialTag() {
        return metadataMaterialTag;
    }

    public String getMetadataTypeTag() {
        return metadataTypeTag;
    }

    public String getMetadataMeasureTag() {
        return metadataMeasureTag;
    }

    public String getMetadataNumPagesTag() {
        return metadataNumPagesTag;
    }

    public String getMetadataYearStartTag() {
        return metadataYearStartTag;
    }

    public String getMetadataYearEndTag() {
        return metadataYearEndTag;
    }

    public String getMetadataTextsTag() {
        return metadataTextsTag;
    }

    public String getMetadataTextsLinesPerColTag() {
        return metadataTextsLinesPerColTag;
    }

    public String getMetadataTextsColsPerPageTag() {
        return metadataTextsColsPerPageTag;
    }

    public String getMetadataTextsLeavesPerGatheringTag() {
        return metadataTextsLeavesPerGatheringTag;
    }

    public String getMetadataTextsNumPagesTag() {
        return metadataTextsNumPagesTag;
    }

    public String getMetadataTextsIdTag() {
        return metadataTextsIdTag;
    }

    public String getMetadataTextsTitleTag() {
        return metadataTextsTitleTag;
    }

    public String getMetadataTextsLocusTag() {
        return metadataTextsLocusTag;
    }

    public String getMetadataTextsFirstPageTag() {
        return metadataTextsFirstPageTag;
    }

    public String getMetadataTextsLastPageTag() {
        return metadataTextsLastPageTag;
    }

    public String getAnnotationSchemaUrl() {
        return annotationSchemaUrl;
    }

    public String getAnnotationDtdUrl() {
        return annotationDtdUrl;
    }
}
