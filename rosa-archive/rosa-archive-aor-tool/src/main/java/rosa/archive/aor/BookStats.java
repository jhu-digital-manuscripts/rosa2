package rosa.archive.aor;

import java.util.HashMap;
import java.util.Map;

/**
 * Aggregated stats for a collection of books.
 */
public class BookStats {

    final String collectionId;
    // Book ID -> book stats
    final Map<String, Stats> statsMap;

    public BookStats(String collectionId) {
        this.collectionId = collectionId;
        this.statsMap = new HashMap<>();
    }

    /**
     * Update the stats for a book with the stats from a single page.
     *
     * @param bookId .
     * @param pageStats stats for one page
     */
    public void addPageStats(String bookId, Stats pageStats) {
        Stats bookStats = statsMap.get(bookId);

        if (bookStats == null) {
            bookStats = new Stats(bookId);
            statsMap.put(bookId, bookStats);
        }

        bookStats.add(pageStats);
    }

}
