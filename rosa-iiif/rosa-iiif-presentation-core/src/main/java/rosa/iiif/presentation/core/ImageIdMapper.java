package rosa.iiif.presentation.core;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

/**
 * Reconciles differences between archive IDs and image server IDs
 */
public interface ImageIdMapper {
    /**
     * Resolve the ID of an image on an image server, given the image in
     * the archive and its location in terms of the collection and book
     * to which it belongs.
     *
     * @param collection
     *          book collection in the archive that the image belongs to
     * @param book
     *          book in the archive that the image belongs to
     * @param imageId
     *          ID of the image in the archive
     * @return the ID of the image on an image server
     */
    String mapId(BookCollection collection, Book book, String imageId);
}
