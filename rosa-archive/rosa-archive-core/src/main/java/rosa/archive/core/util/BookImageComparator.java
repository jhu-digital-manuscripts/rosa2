package rosa.archive.core.util;

import rosa.archive.model.BookImage;
import rosa.archive.model.BookImageRole;

import java.util.Comparator;

/**
 * Compares {@link rosa.archive.model.BookImage} objects
 */
public class BookImageComparator implements Comparator<BookImage> {
    private static BookImageComparator instance;

    private BookImageComparator() {}

    /**
     * @return the instance of this comparator
     */
    public static BookImageComparator instance() {
        if (instance == null) {
            instance = new BookImageComparator();
        }
        return instance;
    }

    @Override
    public int compare(BookImage image1, BookImage image2) {
        if (image1 == null || image2 == null) {
            return 0;
        }

        int difference = getPosition(image1) - getPosition(image2);
        return difference == 0 ? image1.getId().compareToIgnoreCase(image2.getId()) : difference;
    }

    private int getPosition(BookImage image) {
        boolean hasRole = image.getRole() != null;
        if (image.getLocation() == null) {
            return 0;
        }

        switch (image.getLocation()) {
            case FRONT_MATTER:
                if (hasRole && image.getRole() == BookImageRole.PASTEDOWN) {
                    return 1;
                } else {
                    return 2;
                }
            case BODY_MATTER:
                return 3;
            case END_MATTER:
                return 4;
            case BINDING:
                if (hasRole && image.getRole() == BookImageRole.FRONT_COVER) {
                    return 0;
                } else if (hasRole && image.getRole() == BookImageRole.BACK_COVER) {
                    return 5;
                } else {
                    return 6;
                }
            case MISC:
                return 7;
            default:
                return 0;
        }
    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }

}
