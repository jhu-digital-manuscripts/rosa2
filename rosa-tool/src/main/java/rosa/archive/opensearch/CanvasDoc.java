package rosa.archive.opensearch;

/**
 * An Opensearch document representing a single page or canvas in the {@code canvases} index.
 *
 * <p>Each canvas corresponds to one page image within a book and records its
 * position in the page sequence.
 *
 * @param id           the unique document identifier
 * @param manifestId   the identifier of the manifest (book) this canvas belongs to
 * @param collectionId the identifier of the collection this canvas belongs to
 * @param label        the human-readable label for this page
 * @param imageName    the short image identifier for page-based lookups
 * @param position     the 1-based sequential position of this page within the book
 */
public record CanvasDoc(
        String id,
        String manifestId,
        String collectionId,
        String label,
        String imageName,
        int position) {}
