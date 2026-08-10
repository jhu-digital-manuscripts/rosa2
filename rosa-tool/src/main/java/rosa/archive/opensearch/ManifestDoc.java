package rosa.archive.opensearch;

import java.util.List;

/**
 * An Opensearch document representing a single book in the {@code manifest} index.
 *
 * <p>Contains bibliographic metadata and structural information for a digitized
 * manuscript or printed book within the archive.
 *
 * @param id                     the unique document identifier
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
 * @param yearStart              the start year of the date range
 * @param yearEnd                the end year of the date range
 * @param numIllustrations       the number of illustrations in the book
 * @param material               the material of the book (e.g. parchment, paper)
 * @param hasTranscription       whether the book has transcription data available
 * @param thumbnail              up to 3 canvas IDs for representative page thumbnails
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
        int yearStart,
        int yearEnd,
        int numIllustrations,
        String material,
        boolean hasTranscription,
        List<String> thumbnail,
        String logo) {}
