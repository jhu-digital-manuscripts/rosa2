package rosa.archive.opensearch;

import java.util.List;

/**
 * An Opensearch document representing a single book in the {@code manifest} index.
 *
 * <p>Contains bibliographic metadata and structural information for a digitized
 * manuscript or printed book within the archive.
 *
 * <p>The manifest ID format is {@code {collection_id}.{book_id}} (e.g. {@code rose.Douce195}).
 * This provides a globally unique identifier across all collections.
 *
 * @param id                     unique document identifier ({collection_id}.{book_id})
 * @param collectionId           all ancestor collection IDs (immediate + parents)
 * @param label                  the human-readable common name of the book
 * @param title                  the book title (combined from common name + BookText titles)
 * @param repository             the repository holding the physical item
 * @param shelfmark              the repository shelfmark or call number
 * @param date                   the date or date label for the book
 * @param origin                 the place of origin of the book
 * @param numPages               the number of pages (images) in the book
 * @param languages              the languages present in the book
 * @param description            a textual description of the book
 * @param authors                the authors associated with the book (including BookText authors)
 * @param type                   the type of the book (e.g. manuscript, print)
 * @param currentLocation        the current physical location of the book
 * @param numIllustrations       the number of illustrations in the book
 * @param material               the material of the book (e.g. parchment, paper)
 * @param hasTranscription       whether the book has transcription data available
 * @param thumbnail              up to 3 thumbnail objects with iiif_image_id and page_num for representative pages
 * @param logo                   logo image filename for this book
 */
public record ManifestDoc(
        String id,
        List<String> collectionId,
        String label,
        String title,
        String repository,
        String shelfmark,
        String date,
        String origin,
        int numPages,
        List<String> languages,
        String description,
        List<String> authors,
        String type,
        String currentLocation,
        int numIllustrations,
        String material,
        boolean hasTranscription,
        List<ManifestDoc.Thumbnail> thumbnail,
        String logo) {

    /**
     * A thumbnail entry representing a page suitable for use as a book thumbnail.
     *
     * @param iiifImageId the IIIF Image API identifier (e.g. "rose/Douce195/cropped/Douce195.001r")
     * @param pageNum     the 0-based page position in the book (maps to IIIF canvas URI)
     */
    public record Thumbnail(String iiifImageId, int pageNum) {}
}
