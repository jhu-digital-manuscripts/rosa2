package rosa.archive.core;

import rosa.archive.model.BookImage;
import rosa.archive.model.ImageType;

public class ArchiveNameParser {
    private static final String DEFAULT_PAGE_REGEX = "\\d+(r|v|R|V)";
    private static final String DEFAULT_DELIMITER = "\\.";

    private final String delimiter;
    private final String page_regex;

    public ArchiveNameParser(String page_regex, String delimiter) {
        this.page_regex = page_regex == null ? DEFAULT_PAGE_REGEX : page_regex;
        this.delimiter = delimiter == null ? DEFAULT_DELIMITER : delimiter;
    }

    public ArchiveNameParser() {
        this(null, null);
    }

    /**
     * @param name name of an image in the archive
     * @return the image type it belongs to
     */
    public ImageType type(String name) {
        for (ImageType cat : ImageType.values()) {
            if (name.toUpperCase().contains(cat.toString())) {
                return cat;
            }
        }
        if (page(name) != null) {
            return ImageType.TEXT;
        }
        return ImageType.UNKNOWN;
    }

    public ImageType type(BookImage image) {
        return type(image.getId());
    }

    /**
     * Get the page number of the image. Some image categories do not have page
     * numbers, in which case, NULL will be returned.
     *
     * @param name name of an image in the archive
     * @return the page number of the image, or NULL if not applicable
     */
    public String page(String name) {
        String[] parts = split_name(name);

        for (String part : parts) {
            if (part.matches(page_regex)) {
                int n = Integer.parseInt(part.substring(0, part.length()-1));
                char rv = part.charAt(part.length() - 1);

                return String.format("%03d", n) + rv;
            }
        }

        return null;
    }

    /**
     * @param name name of an image in the archive
     * @return the ID of the book this image belongs to
     */
    public String bookId(String name) {
        String[] parts = split_name(name);
        if (parts.length < 3) {
            return null;
        }

        return parts[0];
    }

    private String[] split_name(String name) {
        return name.split(delimiter);
    }
}
