package rosa.archive.core;

/**
 * File name constants for RRDL archive.
 */
public interface RoseFileNames {
    // Collection level (file names)
    String NARRATIVE_SECTIONS = "narrative_sections.csv";
    String CHARACTER_NAMES = "character_names.csv";
    String MISSING_IMAGE = "missing_image.tif";
    String MISSING_FOLIOS = "missing.txt";

    // Book level (file suffix)
    String CROP = ".crop.txt";
    // Descriptions
    String IMAGES = ".images.csv";
    String IMAGES_CROP = ".images.crop.csv";
    String IMAGE_TAGGING = ".imagtag.csv";
    String NARRATIVE_TAGGING = ".nartag.csv";
    String REDUCED_TAGGING = ".redtag.txt";
    // Permissions
    String SHA1SUM = ".SHA1SUM";
    // Transcription text files
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
}
