package rosa.archive.model.aor;

/**
 * A header row element within a table annotation.
 *
 * @param label      the header label
 * @param anchorText the anchor text in the printed book
 * @param anchorData additional anchor data
 * @param content    the header text content
 */
public record TableHeader(String label, String anchorText, String anchorData, String content) {}
