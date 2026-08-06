package rosa.archive.model.aor;

/**
 * A cell within a table annotation.
 *
 * @param row        the row index (0-based)
 * @param col        the column index (0-based)
 * @param anchorText the anchor text in the printed book
 * @param anchorData additional anchor data
 * @param content    the cell text content
 */
public record TableCell(int row, int col, String anchorText, String anchorData, String content) {}
