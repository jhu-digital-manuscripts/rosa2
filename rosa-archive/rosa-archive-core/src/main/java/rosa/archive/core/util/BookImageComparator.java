package rosa.archive.core.util;

import rosa.archive.model.BookImage;

import java.util.Comparator;

/**
 * Compares {@link rosa.archive.model.BookImage} objects
 */
public class BookImageComparator implements Comparator<BookImage> {
    private static final String frontcover = "frontcover";
    private static final String frontmatter = "frontmatter";
    private static final String pastedown = "pastedown";
    private static final String endmatter = "endmatter";
    private static final String backcover = "backcover";
    private static final String misc = "misc";

    private static final String folioMatcher = "\\d+(r|v)";

    private static BookImageComparator instance;

    private BookImageComparator() {}

    public static BookImageComparator instance() {
        if (instance == null) {
            instance = new BookImageComparator();
        }
        return instance;
    }

    @Override
    public int compare(BookImage image1, BookImage image2) {
        String[] i1 = image1.getId().split("\\.");
        String[] i2 = image2.getId().split("\\.");

        try {
            // Front and back covers
            if (i1[2].equals(frontcover)) {
                return -1;
            } else if (i2[2].equals(frontcover)) {
                return 1;
            } else if (i1[2].equals(backcover)) {
                return 1;
            } else if (i2[2].equals(backcover)) {
                return -1;
            }

            // both are main folios
            if (i1[1].matches(folioMatcher) && i2[1].matches(folioMatcher)) {
                return i1[1].compareToIgnoreCase(i2[1]);
            }

            // one or both are frontmatter
            if (i1[1].equals(frontmatter) && !i2[1].equals(frontmatter)) {
                // only i1 is frontmatter
                return -1;
            } else if (!i1[1].equals(frontmatter) && i2[1].equals(frontmatter)) {
                // only i2 is frontmatter
                return 1;
            } else if (i1[1].equals(frontmatter) && i2[1].equals(frontmatter)) {
                // both are frontmatter
                if (i1[2].equals(pastedown)) {
                    // i1 is pastedown
                    return -1;
                } else if (i2[2].equals(pastedown)) {
                    // i2 is pastedown
                    return 1;
                } else if (i1[3].matches(folioMatcher) && i2[3].matches(folioMatcher)) {
                    return i1[3].compareToIgnoreCase(i2[3]);
                }
            }

            // one or both are endmatter
            if (i1[1].equals(endmatter) && !i2[1].equals(endmatter)) {
                // only i1 is endmatter
                return 1;
            } else if (!i1[1].equals(endmatter) && i2[1].equals(endmatter)) {
                // only i2 is endmatter
                return -1;
            } else if (i1[1].equals(endmatter) && i2[1].equals(endmatter)) {
                // both are endmatter
                if (i1[2].equals(pastedown)) {
                    return 1;
                } else if (i2[2].equals(pastedown)) {
                    return -1;
                } else {
                    return i1[3].compareToIgnoreCase(i2[3]);
                }
            }

            if (i1[1].equals(misc) && !i2[1].equals(misc)) {
                // only i1 is misc
                return 1;
            } else if (!i1[1].equals(misc) && i2[1].equals(misc)) {
                // only i2 is misc
                return -1;
            } // if both are misc, use default behavior

        } catch (IndexOutOfBoundsException e) {

        }

        return image1.getId().compareToIgnoreCase(image2.getId());
    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }

}
