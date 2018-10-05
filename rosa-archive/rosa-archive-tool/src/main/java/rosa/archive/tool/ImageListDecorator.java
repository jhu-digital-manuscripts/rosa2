package rosa.archive.tool;

import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.Store;
import rosa.archive.core.serialize.ImageListSerializer;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.ImageList;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This is basically designed to be a run-once utility.
 *
 * The point of this class is to record page labels that are currently being used for each page image
 * for each book in the AOR corpus. Past behavior was to use page labels pulled from the 'signature' or
 * 'pagination' properties in each AOR transcription file. However, since not every image has an
 * associated transcription (there may not be any annotations on a page), not all pages have labels. In
 * this case, the viewer displays our internal name for the page. This lead to a confusing mix of naming
 * conventions within a book, making navigation difficult at times.
 *
 * This tool will read transcriptions as the IIIF endpoint does, then add the page label to the book's
 * Image List. This label can then be read from this list instead of the transcription page. More
 * importantly, it will allow others to fill in missing page labels or correct page labels more easily.
 *
 * If updated page labels are available in the form of an edited ImageList (*.images.csv), one can
 * simply import the image list into the appropriate book. A rebuild of the infrastructure is then
 * needed to update page labels for the viewer.
 *
 * [
 *  Strictly speaking, you would not necessarily need to completely rebuild the IIIF endpoint to capture
 *  the updated page labels. You could directly copy the new image list into the deployed WAR. At this
 *  point, the apps cache would prevent you from seeing the updated labels, so the easiest thing would be
 *  to stop the server, delete the WAR file, the start the server back up. This will refresh the webapp
 *  while preventing the server from re-deploying the WAR. Of course, you could always unpack the WAR,
 *  add the new data, then repack and redeploy it :)
 * ]
 */
public class ImageListDecorator {
    private static final ImageListSerializer imageListSerializer = new ImageListSerializer();

    private PrintStream report;
    private Store store;

    private ByteStreamGroup archive;

    private List<String> errors;

    public ImageListDecorator(Store store, ByteStreamGroup archiveBSG, PrintStream report) {
        this.report = report;
        this.store = store;
        this.archive = archiveBSG;

        this.errors = new ArrayList<>();
    }

    public void run(String collection, String book) {
        try {
            BookCollection col = store.loadBookCollection(collection, errors);

            if (col == null) {
                report.println("Failed to load collection. [" + collection + "]");
                errors.forEach(error -> report.println("  > " + error));
                return;
            }

            if (book != null) {
                doBook(col, book);
            } else {
                Arrays.stream(store.listBooks(collection)).forEach(b -> doBook(col, b));
            }
        } catch (IOException e) {
            report.println("Failed to read collection. [" + collection + "]");
            e.printStackTrace(report);
        }
    }

    /**
     *
     * @param book book object
     */
    private void doBook(BookCollection col, String book) {
        if (book == null) {
            return;
        }

        final String bookLabel = col.getId() + ":" + book;

        Book b;
        try {
            b = store.loadBook(col, book, errors);
        } catch (IOException e) {
            report.println("Failed to read book. [" + bookLabel + "]");
            return;
        }

        if (b == null) {
            report.println("Book not loaded");
            errors.forEach(error -> report.println("  > " + error));
            return;
        }

        ImageList images = b.getImages();
        if (images == null) {
            report.println(" ??? No image list was loaded for book. [" + bookLabel + "]");
            return;
        }

        b.getAnnotatedPages().forEach(aor -> {
            String imageId = aor.getPage();
            String pageLabel = "";

            if (hasContent(aor.getPagination())) {
                pageLabel = aor.getPagination();
            } else if (hasContent(aor.getSignature())) {
                pageLabel = aor.getSignature();
            }

            // Do something in case it fails?
            modifyImageList(imageId, pageLabel, images);
        });

        // Write out ImageList to the right place
        writeImageList(col.getId(), book, images);
    }

    private void modifyImageList(String imageId, String pageLabel, ImageList images) {
        if (images.getImages().stream().noneMatch(image -> image.getId().equals(imageId))) {
            report.println("  > Image ID not found in image list. [" + imageId + "]");
            return;
        }
        images.getImages().stream().filter(image -> image.getId().equals(imageId))
                .forEach(image -> image.setName(pageLabel));
    }

    /**
     * Use crappy workaround using ByteStreamGroups to write ImageList to file because
     * the store doesn't write for you :)
     *
     * @param col book collection
     * @param book book
     * @param images ImageList obj
     */
    private void writeImageList(String col, String book, ImageList images) {
        final String bookLabel = col + ":" + book;
        try (OutputStream out = archive.getByteStreamGroup(col).getByteStreamGroup(book).getOutputStream(images.getId())) {
            imageListSerializer.write(images, out);
        } catch (IOException e) {
            report.println("Failed to write image list [" + bookLabel + "]");
        }
    }

    private boolean hasContent(String str) {
        return str != null && !str.isEmpty();
    }
}
