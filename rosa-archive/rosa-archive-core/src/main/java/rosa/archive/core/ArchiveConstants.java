package rosa.archive.core;

import java.nio.charset.Charset;

public interface ArchiveConstants {
    Charset UTF_8 = Charset.forName("UTF-8");
    
    // AoR annotation schema and dtd

//    String annotationSchemaUrl = "http://www.livesandletters.ac.uk/schema/aor_20141118.xsd";
    String annotationSchemaUrl = "http://www.livesandletters.ac.uk/schema/aor2_18112016.xsd";

    String TEI_SCHEMA_URL = "http://www.tei-c.org/release/xml/tei/custom/schema/xsd/tei_ms.xsd";

    String annotationDtdUrl = "http://www.livesandletters.ac.uk/schema/aor_20141023.dtd";

    // Bytestream names

    String ILLUSTRATION_TITLES = "illustration_titles.csv";

    String NARRATIVE_SECTIONS = "narrative_sections.csv";

    String CHARACTER_NAMES = "character_names.csv";

    String MISSING_IMAGE = "missing_image.tif";

    String CROP = ".crop.txt";

    String IMAGES = ".images.csv";
    
    String METADATA = ".metadata.xml";

    String IMAGES_CROP = ".images.crop.csv";

    String IMAGE_TAGGING = ".imagetag.csv";

    String NARRATIVE_TAGGING = ".nartag.csv";

    String NARRATIVE_TAGGING_MAN = ".nartag.txt";

    String REDUCED_TAGGING = ".redtag.txt";

    String SHA1SUM = ".SHA1SUM";

    String TRANSCRIPTION = ".transcription";

    String DESCRIPTION = ".description_";

    String PERMISSION = ".permission_";

    String BNF_FILEMAP = ".bnf.filemap.csv";

    String BNF_FOLIATION = ".bnf.foliation.xml";

    String BNF_MD5SUM = ".bnf.MD5SUM";

    String AOR_ANNOTATION = ".aor";

    String FILE_MAP = "filemap.csv";

    String COLLECTION_CONFIG = "config.properties";
    
    // Images names

    String IMG_FRONTCOVER = ".binding.frontcover.tif";

    String IMG_BACKCOVER = ".binding.backcover.tif";

    String IMG_FRONTPASTEDOWN = ".frontmatter.pastedown.tif";

    String IMG_ENDPASTEDOWN = ".endmatter.pastedown.tif";

    String IMG_FRONT_FLYLEAF = ".frontmatter.flyleaf.";

    String IMG_END_FLYLEAF = ".endmatter.flyleaf.";

    String CROPPED_DIR = "cropped";

    String MISSING_PREFIX = "*";

    // AoR Specific

    String PEOPLE = "people.csv";

    String LOCATIONS = "locations.csv";

    String BOOKS = "books.csv";

    String ID_LOCATION_MAP = "id_locations.csv";

    // File extensions

    String CSV_EXT = ".csv";

    String XML_EXT = ".xml";
    
    String HTML_EXT = ".html";

    String TXT_EXT = ".txt";

    String TIF_EXT = ".tif";

    // Collection config properties
    String CONFIG_LABEL = "label";
    String CONFIG_DESCRIPTION = "description";
    String CONFIG_MISSING_HEIGHT = "missing_image.height";
    String CONFIG_MISSING_WIDTH = "missing_image.width";
    String CONFIG_PARENTS = "parents";
    String CONFIG_CHILDREN = "children";
    String CONFIG_LANGUAGES = "languages";
    String CONFIG_LOGO = "logo";
}
