package rosa.archive.core.check;

import org.apache.commons.lang3.StringUtils;
import rosa.archive.model.Book;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookText;
import rosa.archive.model.ImageList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Checks data for a {@link rosa.archive.model.Book}
 */
public class BookChecker implements Checker<Book > {

    BookChecker() {  }

    @Override
    public boolean checkBits(Book book) {
        return false;
    }

    @Override
    public boolean checkContent(Book book) {
        List<String> errors = new ArrayList<>();

        errors.addAll(checkContentsOfMetadata(book.getBookMetadata(), book));
        errors.addAll(checkImageList(book.getImages(), book));
        errors.addAll(checkImageList(book.getCroppedImages(), book));

        return errors.isEmpty();
    }

    /**
     * Checks an object for any non-initialized fields. These fields will have NULL values,
     * or equivalent. The field must have an appropriate getter method in order to be
     * checked by this method.
     *
     * TODO kind of crappy, might not work in all cases...plus it's slow!
     *
     * @param obj the object to check
     * @return list of errors found during the check
     */
    protected List<String> checkContentsOfObject(Object obj) {
        List<String> errors = new ArrayList<>();

        Method[] methods = obj.getClass().getMethods();
        for (Method method : methods) {
            if (method.getName().startsWith("get") && method.getParameterCount() == 0) {
                try {
                    Object result = method.invoke(obj);
                    String name = method.getName().substring(3);

                    if (result == null
                            || StringUtils.isBlank(result.toString())
                            || result.toString().equals("-1")) {
                        errors.add(name + " not set.");
                    }
                    // Does not handle complex objects or arrays

                } catch (IllegalAccessException | InvocationTargetException e) {
                    errors.add("Could not access method: [" + method.getName() + "].");
                }
            }
        }

        return errors;
    }

    private boolean isInArchive(String id, String[] contents) {
        return Arrays.binarySearch(contents, id) >= 0;
    }

    /**
     * Check data consistency of a {@link rosa.archive.model.BookMetadata} object according
     * to the rules:
     *
     * <ul>
     *     <li>The metadata must exist as a data model object.</li>
     *     <li>The associated stored metadata file must exist in the archive and be readable.</li>
     *     <li>The metadata fields must not be NULL.</li>
     *     <li>The {@code yearStart} field must fall logically before the {@code yearEnd}.</li>
     * </ul>
     *
     * @param metadata data to check
     * @param parent parent container
     * @return list of errors found during the check
     */
    private List<String> checkContentsOfMetadata(BookMetadata metadata, Book parent) {
        List<String> errors = new ArrayList<>();

        if (metadata == null) {
            errors.add("Metadata is missing.");
            return errors;
        }

        boolean inContent = isInArchive("", parent.getContent());
        // TODO need a way for the Metadata to hold its (file) name. create an ID field?
//        errors.addAll(checkContentsOfObject(metadata));

        if (StringUtils.isBlank(metadata.getDate())) {
            errors.add("Metadata date not set.");
        }
        if (metadata.getYearStart() == -1) {
            errors.add("Metadata start year not set.");
        }
        if (metadata.getYearEnd() - metadata.getYearStart() < 0) {
            errors.add("Date range ends before it begins! " +
                    "Check the description <date notBefore=\"..\" notAfter=\"..\"> element.");
        }
        if (metadata.getYearEnd() == -1) {
            errors.add("Metadata end year not set.");
        }
        if (StringUtils.isBlank(metadata.getCurrentLocation())) {
            errors.add("Metadata current location not set.");
        }
        if (StringUtils.isBlank(metadata.getRepository())) {
            errors.add("Metadata repository not set.");
        }
        if (StringUtils.isBlank(metadata.getShelfmark())) {
            errors.add("Metadata shelfmark not set.");
        }
        if (StringUtils.isBlank(metadata.getOrigin())) {
            errors.add("Metadata origin not set.");
        }
        if (StringUtils.isBlank(metadata.getDimensions())) {
            errors.add("Metadata dimensions not set.");
        }
        if (metadata.getWidth() == -1) {
            errors.add("Metadata width not set.");
        }
        if (metadata.getHeight() == -1) {
            errors.add("Metadata height not set.");
        }
        if (metadata.getNumberOfIllustrations() == -1) {
            errors.add("Metadata number of illustrations not set.");
        }
        if (metadata.getNumberOfPages() == -1) {
            errors.add("Metadata number of pages/folios not set.");
        }
        if (StringUtils.isBlank(metadata.getType())) {
            errors.add("Metadata type not set.");
        }
        if (StringUtils.isBlank(metadata.getCommonName())) {
            errors.add("Metadata common name not set.");
        }
        if (StringUtils.isBlank(metadata.getMaterial())) {
            errors.add("Metadata material not set.");
        }
        if (metadata.getTexts() == null) {
            errors.add("Metadata texts not set.");
        }

        for (BookText text : metadata.getTexts()) {
            if (StringUtils.isBlank(text.getFirstPage())) {
                errors.add("Metadata text first page not set. [" + text + "]");
            }
            if (StringUtils.isBlank(text.getLastPage())) {
                errors.add("Metadata text last page not set. [" + text + "]");
            }
            if (StringUtils.isBlank(text.getTitle())) {
                errors.add("Metadata text title not set. [" + text + "]");
            }
            if (text.getNumberOfIllustrations() == -1) {
                errors.add("Metadata number of illustrations not set. [" + text + "]");
            }
            if (text.getNumberOfPages() == -1) {
                errors.add("Metadata number of pages not set. [" + text + "]");
            }
            if (text.getColumnsPerPage() == -1) {
                errors.add("Metadata columns per page not set. [" + text + "]");
            }
            if (text.getLeavesPerGathering() == -1) {
                errors.add("Metadata leaves per gathering not set. [" + text + "]");
            }
            if (text.getLinesPerColumn() == -1) {
                errors.add("Metadata lines per column not set. [" + text + "]");
            }
        }

        return errors;
    }

    /**
     * Check data consistency of an {@link rosa.archive.model.ImageList} according
     * to the following rules:
     *
     * <ul>
     *     <li>The image list must exist.</li>
     *     <li>Each image in the list must be initialized with non-null field values.</li>
     *     <li>Any image marked as missing must not appear in the Book's list of contents.</li>
     *     <li>Any image marked as not missing must appear in the Book's list of contents.</li>
     * </ul>
     *
     * @param images {@link rosa.archive.model.ImageList}
     * @param parent {@link rosa.archive.model.Book} that contains this image list
     * @return list of errors found while checking data
     */
    private List<String> checkImageList(ImageList images, Book parent) {
        List<String> errors = new ArrayList<>();

        if (images == null || images.getImages() == null) {
            errors.add("Image list is missing.");
            return errors;
        }

        for (BookImage image : images.getImages()) {
            // Make sure the width, height, ID fields were set
//            errors.addAll(checkContentsOfObject(image));
            if (StringUtils.isBlank(image.getId())) {
                errors.add("Image ID not set. [" + image + "]");
            }
            if (image.getWidth() == -1) {
                errors.add("Image width not set. [" + image + "]");
            }
            if (image.getHeight() == -1) {
                errors.add("Image height not set. [" + image + "]");
            }

            // Relies on the fact that the array is sorted.
            boolean inContent = isInArchive(image.getId(), parent.getContent());
            if (image.isMissing() && inContent) {
                errors.add("Image [" + image.getId() + "] marked as missing is present in archive.");
            } else if (!image.isMissing() && !inContent) {
                errors.add("Image [" + image.getId() + "] marked as present in archive is missing.");
            }
        }

        return errors;
    }
}
