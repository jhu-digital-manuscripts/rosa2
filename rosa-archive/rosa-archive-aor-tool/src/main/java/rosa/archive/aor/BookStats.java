package rosa.archive.aor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Aggregated stats for a collection of books.
 */
public class BookStats {

    // Book ID -> book stats
    final Map<String, Stats> statsMap;
    // Book ID -> set of unreadable page IDs
    final Map<String, Set<String>> unreadablePagesMap;

    public BookStats() {
        this.statsMap = new HashMap<>();
        this.unreadablePagesMap = new HashMap<>();
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

    /**
     * Mark a page as unreadable for the given book. Ignore if the page
     * is already marked as unreadable.
     *
     * @param bookId .
     * @param pageId .
     */
    public void addUnreadablePage(String bookId, String pageId) {
        Set<String> pageSet = unreadablePagesMap.get(bookId);

        if (pageSet != null) {
            pageSet.add(pageId);
        } else {
            pageSet = new HashSet<>();
            pageSet.add(pageId);
            unreadablePagesMap.put(bookId, pageSet);
        }
    }

    public int getNumberOfUnreadablePages(String bookId) {
        return unreadablePagesMap.get(bookId) != null ? unreadablePagesMap.get(bookId).size() : 0;
    }

    public int getNumberOfUnreadablePages() {
        int count = 0;

        for (String book : unreadablePagesMap.keySet()) {
            count += getNumberOfUnreadablePages(book);
        }

        return count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BookStats stats = (BookStats) o;
        return statsMap.equals(stats.statsMap) && unreadablePagesMap.equals(stats.unreadablePagesMap);

    }

    @Override
    public int hashCode() {
        return 31 * statsMap.hashCode() + unreadablePagesMap.hashCode();
    }

    @Override
    public String toString() {
        return "BookStats{" + "statsMap=" + statsMap + ", unreadablePagesMap=" + unreadablePagesMap + '}';
    }
}
