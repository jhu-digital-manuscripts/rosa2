package rosa.archive.core;

/**
 * File name constants for RRDL archive. TODO make this stuff configurable!!
 */
public interface RoseConstants {
    String CHARSET = "UTF-8";

    // Collection level (file names)
    String NARRATIVE_SECTIONS = "narrative_sections.csv";
    String CHARACTER_NAMES = "character_names.csv";
    String MISSING_IMAGE = "missing_image.tif";
    String MISSING_FOLIOS = "missing.txt";

    // Book level (file suffix)
    String CROP = ".crop.txt";
    String DESCRIPTION = ".description_"; // Needs a language code and file extension
    String IMAGES = ".images.csv";
    String IMAGES_CROP = ".images.crop.csv";
    String IMAGE_TAGGING = ".imagetag.csv";
    String AUTOMATIC_NARRATIVE_TAGGING = ".nartag.csv";
    String MANUAL_NARRATIVE_TAGGING = ".nartag.txt";
    String REDUCED_TAGGING = ".redtag.txt";
    String PERMISSION = ".permission_"; // Needs a language code and file extension
    String SHA1SUM = ".SHA1SUM";
    String TRANSCRIPTION_TXT = ".transcription."; // Needs folio name and file extension
    String TRANSCRIPTION = ".transcription.xml";
    String BNF_FILEMAP = ".bnf.filemap.csv";
    String BNF_FOLIATION = ".bnf.foliation.xml";
    String BNF_MD5SUM = ".bnf.MD5SUM";

    // Images
    String IMG_FRONTCOVER = ".binding.frontcover.tif";
    String IMG_BACKCOVER = ".binding.backcover.tif";
    String ING_FRONTPASTEDOWN = ".frontmatter.pastedown.tif";
    String IMG_ENDPASTEDOWN = ".endmatter.pastedown.tif";
    String IMG_FRONT_FLYLEAF = ".frontmatter.flyleaf.";
    String IMG_END_FLYLEAF = ".endmatter.flyleaf.";
    String CROPPED_DIR = "cropped";

    String MISSING_PREFIX = "*";

    // File extensions
    String CSV = ".csv";
    String TXT = ".txt";
    String XML = ".xml";
    String HTML = ".html";
}
