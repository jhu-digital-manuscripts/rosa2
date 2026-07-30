package rosa.archive.core;

import rosa.archive.model.BookImageLocation;
import rosa.archive.model.BookImageRole;

/**
 * Parses archive item names to extract location, role, page, and short name
 * information. Uses a dot-delimited naming convention.
 */
public final class ArchiveNameParser {

    private static final String DEFAULT_PAGE_REGEX = "(([a-zA-Z]*)(\\d+)([rRvV]))|(\\d+)";
    private static final String DEFAULT_DELIMITER = "\\.";

    /** Prefix marking a missing image in image lists. */
    public static final String MISSING_PREFIX = "*";

    /** Prefix marking an auto-generated name in image lists. */
    public static final String GENERATED_PREFIX = "##";

    private final String delimiter;
    private final String pageRegex;

    /**
     * Creates an ArchiveNameParser with default settings.
     */
    public ArchiveNameParser() {
        this(null, null);
    }

    /**
     * Creates an ArchiveNameParser with custom settings.
     *
     * @param pageRegex  regex identifying pagination segments, or null for default
     * @param delimiter  delimiter string for splitting names, or null for default (dot)
     */
    public ArchiveNameParser(String pageRegex, String delimiter) {
        this.pageRegex = pageRegex == null ? DEFAULT_PAGE_REGEX : pageRegex;
        this.delimiter = delimiter == null ? DEFAULT_DELIMITER : delimiter;
    }

    /**
     * Determines the physical location of an image in the book from its archive ID.
     *
     * @param imageId the image identifier
     * @return the location, or null if unrecognized
     */
    public BookImageLocation location(String imageId) {
        String[] parts = splitName(imageId);

        if (parts.length < 3) {
            return null;
        }

        String location = parts[1];
        for (BookImageLocation loc : BookImageLocation.values()) {
            if (loc.getInArchiveName().equals(location)) {
                return loc;
            }
        }

        if (location.matches(pageRegex) && (parts.length == 3 || role(imageId) == BookImageRole.INSERT)) {
            return BookImageLocation.BODY_MATTER;
        }

        return null;
    }

    /**
     * Determines the role of an image from its archive ID.
     *
     * @param imageId the image identifier
     * @return the role, or null if no recognized role
     */
    public BookImageRole role(String imageId) {
        String[] parts = splitName(imageId);

        for (int i = 1; i < parts.length - 1; i++) {
            for (BookImageRole r : BookImageRole.values()) {
                if (r.getArchiveName().equals(parts[i])) {
                    return r;
                }
            }
        }
        return null;
    }

    /**
     * Generates a short, human-readable label for an image.
     *
     * @param imageId the image identifier
     * @return the short name
     */
    public String shortName(String imageId) {
        BookImageRole imageRole = role(imageId);
        BookImageLocation imageLocation = location(imageId);
        String page = page(imageId);
        String insertNum = insertNumber(imageId);

        var shortName = new StringBuilder();

        if (imageLocation != null) {
            shortName.append(imageLocation.getDisplay()).append(' ');
        }

        if (page != null) {
            shortName.append(page.replaceFirst("^0+(?!$)", "")).append(" ");
        }

        if (imageRole != null) {
            shortName.append(imageRole.getDisplay()).append(' ');
        }

        if (insertNum != null) {
            shortName.append(insertNum);
        }

        return shortName.toString().trim();
    }

    /**
     * Extracts the page number from an image ID.
     *
     * @param imageId the image identifier
     * @return the page number segment, or null if none found
     */
    public String page(String imageId) {
        String[] parts = splitName(imageId);

        for (String part : parts) {
            if (part.matches(pageRegex)) {
                return part;
            }
        }
        return null;
    }

    /**
     * Extracts the insert number from an image ID.
     *
     * @param imageId the image identifier
     * @return the insert number, or null if not an insert
     */
    public String insertNumber(String imageId) {
        String[] parts = imageId.split(delimiter);

        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].equals(BookImageRole.INSERT.getArchiveName())) {
                String possible = parts[i + 1];
                if (possible.matches("\\d+")) {
                    return possible;
                }
            }
        }
        return null;
    }

    /**
     * Tests whether an image ID indicates a missing image.
     *
     * @param imageId the image identifier
     * @return true if the image is marked as missing
     */
    public boolean isMissing(String imageId) {
        return imageId.startsWith(MISSING_PREFIX);
    }

    /**
     * Checks whether a filename is an AoR transcription XML file.
     * AoR transcription files contain ".aor." and end with ".xml".
     *
     * @param name the filename to test
     * @return true if this is an AoR transcription file
     */
    public boolean isAorTranscription(String name) {
        return name.contains(".aor.") && name.endsWith(".xml");
    }

    /**
     * Returns a short image identifier unique within the book.
     *
     * @param imageId the full image ID
     * @return the short unique ID within the book
     */
    public String shortUniqueImageIdInBook(String imageId) {
        if (imageId.equals("missing_image.tif")) {
            return "missing";
        }

        String[] parts = splitName(imageId);
        var result = new StringBuilder();
        for (int i = 1; i < parts.length - 1; i++) {
            result.append(parts[i]);
            if (i != parts.length - 2) {
                result.append('.');
            }
        }
        return result.toString();
    }

    private String[] splitName(String name) {
        return name.split(delimiter);
    }
}
