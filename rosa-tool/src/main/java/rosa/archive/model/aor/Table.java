package rosa.archive.model.aor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An annotation representing a table on a page.
 *
 * <p>Tables have headers, cells, text elements, and may reference people, books,
 * locations, and symbols.</p>
 */
public final class Table extends AbstractAnnotation {

    private List<TableHeader> headers;
    private List<TableCell> cells;
    private List<TextEl> texts;
    private List<String> people;
    private List<String> books;
    private List<String> locations;
    private List<String> symbols;
    private List<InternalReference> internalRefs;
    private String type;
    private String aggregatedInfo;
    private String translation;

    /**
     * Creates a table annotation with the specified fields.
     *
     * @param id       the annotation identifier
     * @param location the physical location on the page
     */
    public Table(String id, Location location) {
        super(id, null, null, location);
        this.headers = new ArrayList<>();
        this.cells = new ArrayList<>();
        this.texts = new ArrayList<>();
        this.people = new ArrayList<>();
        this.books = new ArrayList<>();
        this.locations = new ArrayList<>();
        this.symbols = new ArrayList<>();
        this.internalRefs = new ArrayList<>();
    }

    /**
     * Returns the column headers.
     *
     * @return the headers (never null)
     */
    public List<TableHeader> getColHeaders() {
        return headers;
    }

    /**
     * Sets the table headers.
     *
     * @param headers the headers
     */
    public void setHeaders(List<TableHeader> headers) {
        this.headers = headers;
    }

    /**
     * Returns the header at the specified row index.
     *
     * @param row the row index
     * @return the header at the given index
     */
    public TableHeader getRow(int row) {
        return headers.get(row);
    }

    /**
     * Returns the table cells.
     *
     * @return the cells (never null)
     */
    public List<TableCell> getCells() {
        return cells;
    }

    /**
     * Returns the cell at the specified row and column.
     *
     * @param row the row index
     * @param col the column index
     * @return the matching cell, or null
     */
    public TableCell getCell(int row, int col) {
        return cells.stream()
                .filter(cell -> cell.row() == row && cell.col() == col)
                .findFirst()
                .orElse(null);
    }

    /**
     * Sets the table cells.
     *
     * @param cells the cells
     */
    public void setCells(List<TableCell> cells) {
        this.cells = cells;
    }

    /**
     * Returns the table type.
     *
     * @return the type, or null
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the table type.
     *
     * @param type the type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the aggregated information string.
     *
     * @return the aggregated info, or null
     */
    public String getAggregatedInfo() {
        return aggregatedInfo;
    }

    /**
     * Sets the aggregated information.
     *
     * @param aggregatedInfo the aggregated info
     */
    public void setAggregatedInfo(String aggregatedInfo) {
        this.aggregatedInfo = aggregatedInfo;
    }

    /**
     * Returns the text elements within this table.
     *
     * @return the text elements (never null)
     */
    public List<TextEl> getTexts() {
        return texts;
    }

    /**
     * Sets the text elements.
     *
     * @param texts the text elements
     */
    public void setTexts(List<TextEl> texts) {
        this.texts = texts;
    }

    /**
     * Returns the people referenced.
     *
     * @return the people list (never null)
     */
    public List<String> getPeople() {
        return people;
    }

    /**
     * Sets the people list.
     *
     * @param people the people
     */
    public void setPeople(List<String> people) {
        this.people = people;
    }

    /**
     * Returns the books referenced.
     *
     * @return the books list (never null)
     */
    public List<String> getBooks() {
        return books;
    }

    /**
     * Sets the books list.
     *
     * @param books the books
     */
    public void setBooks(List<String> books) {
        this.books = books;
    }

    /**
     * Returns the locations referenced.
     *
     * @return the locations list (never null)
     */
    public List<String> getLocations() {
        return locations;
    }

    /**
     * Sets the locations list.
     *
     * @param locations the locations
     */
    public void setLocations(List<String> locations) {
        this.locations = locations;
    }

    /**
     * Returns the symbols referenced.
     *
     * @return the symbols list (never null)
     */
    public List<String> getSymbols() {
        return symbols;
    }

    /**
     * Sets the symbols list.
     *
     * @param symbols the symbols
     */
    public void setSymbols(List<String> symbols) {
        this.symbols = symbols;
    }

    /**
     * Returns the internal references.
     *
     * @return the internal references (never null)
     */
    public List<InternalReference> getInternalRefs() {
        return internalRefs;
    }

    /**
     * Sets the internal references.
     *
     * @param internalRefs the internal references
     */
    public void setInternalRefs(List<InternalReference> internalRefs) {
        this.internalRefs = internalRefs;
    }

    /**
     * Returns the translation of this table's text.
     *
     * @return the translation, or null
     */
    public String getTranslation() {
        return translation;
    }

    /**
     * Sets the translation.
     *
     * @param translation the translation
     */
    public void setTranslation(String translation) {
        this.translation = translation;
    }

    @Override
    public String toPrettyString() {
        return "Table{type=" + type + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Table table)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(headers, table.headers) &&
                Objects.equals(cells, table.cells) &&
                Objects.equals(texts, table.texts) &&
                Objects.equals(people, table.people) &&
                Objects.equals(books, table.books) &&
                Objects.equals(locations, table.locations) &&
                Objects.equals(symbols, table.symbols) &&
                Objects.equals(internalRefs, table.internalRefs) &&
                Objects.equals(type, table.type) &&
                Objects.equals(aggregatedInfo, table.aggregatedInfo) &&
                Objects.equals(translation, table.translation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), headers, cells, texts, people, books,
                locations, symbols, internalRefs, type, aggregatedInfo, translation);
    }

    @Override
    public String toString() {
        return "Table{" +
                "headers=" + headers.size() +
                ", cells=" + cells.size() +
                ", texts=" + texts.size() +
                ", type='" + type + '\'' +
                ", aggregatedInfo='" + aggregatedInfo + '\'' +
                ", translation='" + translation + '\'' +
                '}';
    }
}
