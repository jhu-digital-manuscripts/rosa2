package rosa.archive.core;

import java.nio.charset.Charset;

// TODO Capitalize, normalize based on usage
// TODO Not clear all of this needs to be here
public interface ArchiveConstants {
    static final Charset UTF_8 = Charset.forName("UTF-8");
    
    // AoR annotation schema and dtd

    static final String annotationSchemaUrl = "http://www.livesandletters.ac.uk/schema/aor_20141118.xsd";

    static final String annotationDtdUrl = "http://www.livesandletters.ac.uk/schema/aor_20141023.dtd";

    // Bytestream names

    static final String ILLUSTRATION_TITLES = "illustration_titles.csv";

    static final String NARRATIVE_SECTIONS = "narrative_sections.csv";

    static final String CHARACTER_NAMES = "character_names.csv";

    static final String MISSING_IMAGE = "missing_image.tif";

    static final String CROP = ".crop.txt";

    static final String IMAGES = ".images.csv";

    static final String IMAGES_CROP = ".images.crop.csv";

    static final String IMAGE_TAGGING = ".imagetag.csv";

    static final String NARRATIVE_TAGGING = ".nartag.csv";

    static final String NARRATIVE_TAGGING_MAN = ".nartag.txt";

    static final String REDUCED_TAGGING = ".redtag.txt";

    static final String SHA1SUM = ".SHA1SUM";

    static final String TRANSCRIPTION = ".transcription";

    static final String DESCRIPTION = ".description_";

    static final String PERMISSION = ".permission_";

    static final String BNF_FILEMAP = ".bnf.filemap.csv";

    static final String BNF_FOLIATION = ".bnf.foliation.xml";

    static final String BNF_MD5SUM = ".bnf.MD5SUM";

    // Images names

    static final String IMG_FRONTCOVER = ".binding.frontcover.tif";

    static final String IMG_BACKCOVER = ".binding.backcover.tif";

    static final String IMG_FRONTPASTEDOWN = ".frontmatter.pastedown.tif";

    static final String IMG_ENDPASTEDOWN = ".endmatter.pastedown.tif";

    static final String IMG_FRONT_FLYLEAF = ".frontmatter.flyleaf.";

    static final String IMG_END_FLYLEAF = ".endmatter.flyleaf.";

    static final String CROPPED_DIR = "cropped";

    static final String MISSING_PREFIX = "*";

    // File extensions

    static final String CSV_EXT = ".csv";

    static final String XML_EXT = ".xml";

    static final String TXT_EXT = ".txt";

    static final String TIF_EXT = ".tif";
}
