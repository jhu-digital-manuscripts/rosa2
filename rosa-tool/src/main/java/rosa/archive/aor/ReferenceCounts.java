package rosa.archive.aor;

/**
 * Counts of external references (people, books, locations) found in annotations.
 *
 * <p>Used in AoR statistics reporting to summarize how many distinct references
 * to people, books, and locations appear in a book's annotations.
 *
 * @param books     the number of books referenced in annotations
 * @param people    the number of people referenced in annotations
 * @param locations the number of locations referenced in annotations
 */
public record ReferenceCounts(int books, int people, int locations) {}
