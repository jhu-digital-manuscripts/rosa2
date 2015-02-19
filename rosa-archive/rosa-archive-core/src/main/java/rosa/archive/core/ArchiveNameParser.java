package rosa.archive.core;

import rosa.archive.model.ArchiveItemType;
import rosa.archive.model.BookImageLocation;
import rosa.archive.model.BookImageRole;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArchiveNameParser implements ArchiveConstants {
    private static final String DEFAULT_PAGE_REGEX = "([a-zA-Z]*)(\\d+)(r|v|R|V)";
    private static final String DEFAULT_DELIMITER = "\\.";

    private final String delimiter;
    private final String page_regex;

    /**
     * Create an ArchiveNameParser with custom settings.
     *
     * @param page_regex regular expressions identifying pagination
     * @param delimiter string used to delimit segments of an archive item name
     */
    public ArchiveNameParser(String page_regex, String delimiter) {
        this.page_regex = page_regex == null ? DEFAULT_PAGE_REGEX : page_regex;
        this.delimiter = delimiter == null ? DEFAULT_DELIMITER : delimiter;
    }

    /**
     * Create an ArchiveNapeParser with default settings.
     */
    public ArchiveNameParser() {
        this(null, null);
    }

    /**
     * Get the location of the book image from its archive ID. If an unknown name
     * is encountered, NULL is returned.
     *
     * @param imageId ID of image in archive
     * @return the location of the book image
     */
    public BookImageLocation location(String imageId) {
        String[] parts = split_name(imageId);

        if (parts.length < 3) {
            return null;
        }

        String location = parts[1];
        for (BookImageLocation loc : BookImageLocation.values()) {
            if (loc.getInArchiveName().equals(location)) {
                return loc;
            }
        }

        if (parts.length == 3 && location.matches(page_regex)) {
            return BookImageLocation.BODY_MATTER;
        }

        return null;
    }

    /**
     * Get the role of the book image from its archive ID.
     *
     * @param imageId ID of image in archive
     * @return the role of the book image
     */
    public BookImageRole role(String imageId) {
        String[] parts = split_name(imageId);
        if (parts.length < 4) {
            /*
                Body matter images will have length == 3, other images
                will have length > 3
             */
            return null;
        }

        String role = parts[2];
        for (BookImageRole r : BookImageRole.values()) {
            if (r.getArchiveName().equals(role)) {
                return r;
            }
        }

        return null;
    }

    /**
     * Get a short name, a human readable label.
     *
     * @param imageId ID of image in archive
     * @return short name
     */
    public String shortName(String imageId) {
        BookImageRole role = role(imageId);
        BookImageLocation location = location(imageId);
        String page = page(imageId);

        StringBuilder short_name = new StringBuilder();

        if (location != null) {
            short_name.append(location.getDisplay());
            short_name.append(' ');
        }
        if (role != null) {
            short_name.append(role.getDisplay());
            short_name.append(' ');
        }
        // Strip leading zeros from page number
        if (page != null) {
            short_name.append(page.replaceFirst("^0+(?!$)", ""));
        }

        return short_name.toString().trim();
    }

    /**
     * Get the page number associated with an item in the archive. If the
     * item is associated with a part of the book that does not have a
     * page number (ex: front cover), NULL will be returned.
     *
     * @param imageId ID of item in archive
     * @return the page associated with the item
     */
    public String page(String imageId) {
        String[] parts = split_name(imageId);

        for (String part : parts) {
            if (part.matches(page_regex)) {
                return part;
            }
        }

        return null;
    }

    /**
     * Does this name not have an associated image in the archive (is it missing)?
     *
     * @param imageId ID of image in archive
     * @return is the image missing?
     */
    public boolean isMissing(String imageId) {
        return imageId.startsWith(MISSING_PREFIX);
    }

    /**
     * Get the item type for an object in the archive from its name.
     *
     * @param name name if item in archive
     * @return item type
     */
    public ArchiveItemType getArchiveItemType(String name) {
        if (name.startsWith(".")) {
            return null;
        }

        if (name.endsWith(TIF_EXT)) {
            return ArchiveItemType.IMAGE;
        }

        for (ArchiveItemType type : ArchiveItemType.values()) {
            if (name.contains(type.getIdentifier()) && name.endsWith(type.getFileExtension())) {
                return type;
            }
        }

        return null;
    }

    private String[] split_name(String name) {
        return name.split(delimiter);
    }
}
