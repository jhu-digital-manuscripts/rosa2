package rosa.archive.model.aor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * Element representing a table
 *
 * &lt;table type place id internal_ref aggregated_information&gt;
 *   &lt;tr&gt;
 *     &lt;th label anchor_text anchor_data&gt;moo&lt;/th&gt;
 *     &lt;td anchor_text anchor_data&gt;moo&lt;/td&gt;
 *   &lt;/tr&gt;
 *   &lt;text&gt;
 *   &lt;person /&gt;
 *   &lt;book /&gt;
 *   &lt;location /&gt;
 *   &lt;symbol_in_text /&gt;
 *   &lt;internal_ref&gt;
 *   &lt;translation&gt;
 * &lt;/table&gt;
 *
 * Attributes:
 * <ul>
 *   <li>type (required)</li>
 *   <li>place (required)</li>
 *   <li>id (optional)</li>
 *   <li>internal_ref (optional)</li>
 *   <li>aggregated_information (optional)</li>
 * </ul>
 *
 * Contains elements:
 * <ul>
 *   <li>tr (zero or more) : row, containing header information and cells {@link TableRow} {@link TableCell}</li>
 *   <li>text (zero or more) : {@link TextEl}</li>
 *   <li>person (zero or more) : {@link #people}</li>
 *   <li>book (zero or more) : {@link #books}</li>
 *   <li>location (zero or more) : {@link #locations}</li>
 *   <li>symbol_in_text (zero or more) : {@link #symbols}</li>
 *   <li>internal_ref (zero or more) : {@link InternalReference}</li>
 *   <li>translation (zero or one) : {@link #translation}</li>
 * </ul>
 */
public class Table extends Annotation implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<TableRow> rows;
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

    public Table(String id, Location location) {
        super(id, null, null, location);
        this.texts = new ArrayList<>();
        this.rows = new ArrayList<>();
        this.cells = new ArrayList<>();
        this.people = new ArrayList<>();
        this.books = new ArrayList<>();
        this.locations = new ArrayList<>();
        this.symbols = new ArrayList<>();
        this.internalRefs = new ArrayList<>();
    }

    public List<TableRow> getRows() {
        return rows;
    }

    public void setRows(List<TableRow> rows) {
        this.rows = rows;
    }

    public TableRow getRow(int row) {
        return rows.get(row);
    }

    public List<TableCell> getCells() {
        return cells;
    }

    public TableCell getCell(int row, int col) {
        return cells.stream()
                .filter(cell -> cell.row == row && cell.col == col)
                .findFirst()
                .orElse(null);
    }

    public void setCells(List<TableCell> cols) {
        this.cells = cols;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAggregatedInfo() {
        return aggregatedInfo;
    }

    public void setAggregatedInfo(String aggregatedInfo) {
        this.aggregatedInfo = aggregatedInfo;
    }

    public List<TextEl> getTexts() {
        return texts;
    }

    public void setTexts(List<TextEl> texts) {
        this.texts = texts;
    }

    public List<String> getPeople() {
        return people;
    }

    public void setPeople(List<String> people) {
        this.people = people;
    }

    public List<String> getBooks() {
        return books;
    }

    public void setBooks(List<String> books) {
        this.books = books;
    }

    public List<String> getLocations() {
        return locations;
    }

    public void setLocations(List<String> locations) {
        this.locations = locations;
    }

    public List<String> getSymbols() {
        return symbols;
    }

    public void setSymbols(List<String> symbols) {
        this.symbols = symbols;
    }

    public List<InternalReference> getInternalRefs() {
        return internalRefs;
    }

    public void setInternalRefs(List<InternalReference> internalRefs) {
        this.internalRefs = internalRefs;
    }

    public String getTranslation() {
        return translation;
    }

    public void setTranslation(String translation) {
        this.translation = translation;
    }

    @Override
    public String toPrettyString() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Table table = (Table) o;

        if (rows != null ? !rows.equals(table.rows) : table.rows != null) return false;
        if (cells != null ? !cells.equals(table.cells) : table.cells != null) return false;
        if (texts != null ? !texts.equals(table.texts) : table.texts != null) return false;
        if (people != null ? !people.equals(table.people) : table.people != null) return false;
        if (books != null ? !books.equals(table.books) : table.books != null) return false;
        if (locations != null ? !locations.equals(table.locations) : table.locations != null) return false;
        if (symbols != null ? !symbols.equals(table.symbols) : table.symbols != null) return false;
        if (internalRefs != null ? !internalRefs.equals(table.internalRefs) : table.internalRefs != null) return false;
        if (type != null ? !type.equals(table.type) : table.type != null) return false;
        if (aggregatedInfo != null ? !aggregatedInfo.equals(table.aggregatedInfo) : table.aggregatedInfo != null)
            return false;
        return translation != null ? translation.equals(table.translation) : table.translation == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (rows != null ? rows.hashCode() : 0);
        result = 31 * result + (cells != null ? cells.hashCode() : 0);
        result = 31 * result + (texts != null ? texts.hashCode() : 0);
        result = 31 * result + (people != null ? people.hashCode() : 0);
        result = 31 * result + (books != null ? books.hashCode() : 0);
        result = 31 * result + (locations != null ? locations.hashCode() : 0);
        result = 31 * result + (symbols != null ? symbols.hashCode() : 0);
        result = 31 * result + (internalRefs != null ? internalRefs.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (aggregatedInfo != null ? aggregatedInfo.hashCode() : 0);
        result = 31 * result + (translation != null ? translation.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Table{" +
                "rows=" + rows.size() +
                ", cells=" + cells.size() +
                ", texts=" + texts.size() +
                ", people=" + people +
                ", books=" + books +
                ", locations=" + locations +
                ", symbols=" + symbols +
                ", internalRefs=" + internalRefs.size() +
                ", type='" + type + '\'' +
                ", aggregatedInfo='" + aggregatedInfo + '\'' +
                ", translation='" + translation + '\'' +
                '}';
    }
}
