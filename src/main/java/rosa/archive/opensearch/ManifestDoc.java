package rosa.archive.opensearch;

import java.util.List;

/**
 * An Opensearch document representing a single book in the {@code manifests} index.
 *
 * <p>Contains bibliographic metadata and structural information for a digitized
 * manuscript or printed book within the archive.
 *
 * @param id                     the unique document identifier
 * @param collectionId           the identifier of the collection this book belongs to
 * @param title                  the book title or common name
 * @param titles                 all titles including BookText titles
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
 * @param websites               associated website URLs
 * @param yearStart              the start year of the date range
 * @param yearEnd                the end year of the date range
 * @param numberOfIllustrations  the number of illustrations in the book
 * @param material               the material of the book (e.g. parchment, paper)
 * @param hasTranscription       whether the book has transcription data available
 */
public record ManifestDoc(
        String id,
        String collectionId,
        String title,
        List<String> titles,
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
        List<String> websites,
        int yearStart,
        int yearEnd,
        int numberOfIllustrations,
        String material,
        boolean hasTranscription) {}
