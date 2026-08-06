package rosa.archive.aor;

/**
 * Counts of each annotation type and their associated word counts for a book or page.
 *
 * <p>Used in AoR statistics reporting to summarize annotation density across
 * different annotation categories.
 *
 * @param marginalia      the number of marginalia annotations
 * @param marginaliaWords the total word count across all marginalia
 * @param underlines      the number of underline annotations
 * @param underlineWords  the total word count across all underlines
 * @param marks           the number of mark annotations
 * @param markWords       the total word count across all marks
 * @param symbols         the number of symbol annotations
 * @param symbolWords     the total word count across all symbols
 * @param drawings        the number of drawing annotations
 * @param drawingWords    the total word count across all drawings
 * @param numerals        the number of numeral annotations
 * @param calculations    the number of calculation annotations
 * @param graphs          the number of graph annotations
 * @param graphWords      the total word count across all graphs
 * @param tables          the number of table annotations
 * @param tableWords      the total word count across all tables
 * @param physLinks       the number of physical link annotations
 */
public record AnnotationCounts(
        int marginalia,
        int marginaliaWords,
        int underlines,
        int underlineWords,
        int marks,
        int markWords,
        int symbols,
        int symbolWords,
        int drawings,
        int drawingWords,
        int numerals,
        int calculations,
        int graphs,
        int graphWords,
        int tables,
        int tableWords,
        int physLinks) {}
