package rosa.archive.aor;

import java.util.HashMap;
import java.util.Map;

/**
 * Aggregated stats for a collection of books.
 */
public class BookStats {

    // Book ID -> book stats
    final Map<String, Stats> statsMap;

    public BookStats() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BookStats stats = (BookStats) o;
        return statsMap.equals(stats.statsMap);

    }

    @Override
    public int hashCode() {
        return statsMap.hashCode();
    }

    @Override
    public String toString() {
        return "BookStats{" + "statsMap=" + statsMap + '}';
    }
}
