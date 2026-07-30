package rosa.archive.model.aor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Aggregates all annotations on a single page of a book.
 *
 * <p>Contains page metadata (filename, pagination, signature, reader) and lists of
 * each annotation type found on the page.</p>
 */
public final class AnnotatedPage {

    private String id;
    private String page;
    private String reader;
    private String pagination;
    private String signature;
    private String origin;
    private String status;
    private String hostPage;
    private String relatedPage;

    private List<Marginalia> marginalia;
    private List<Mark> marks;
    private List<Symbol> symbols;
    private List<Underline> underlines;
    private List<Numeral> numerals;
    private List<Errata> errata;
    private List<Drawing> drawings;
    private List<Calculation> calculations;
    private List<Graph> graphs;
    private List<Table> tables;
    private List<PhysicalLink> links;

    /**
     * Creates an empty annotated page with initialized lists.
     */
    public AnnotatedPage() {
        marginalia = new ArrayList<>();
        marks = new ArrayList<>();
        symbols = new ArrayList<>();
        underlines = new ArrayList<>();
        numerals = new ArrayList<>();
        errata = new ArrayList<>();
        drawings = new ArrayList<>();
        calculations = new ArrayList<>();
        graphs = new ArrayList<>();
        tables = new ArrayList<>();
        links = new ArrayList<>();
    }

    /**
     * Returns a stream of all annotations on this page.
     *
     * @return a stream over all annotation types
     */
    @SuppressWarnings("unchecked")
    public Stream<Annotation> stream() {
        return Stream.of(
                (Stream<Annotation>) (Stream<?>) marginalia.stream(),
                (Stream<Annotation>) (Stream<?>) marks.stream(),
                (Stream<Annotation>) (Stream<?>) symbols.stream(),
                (Stream<Annotation>) (Stream<?>) underlines.stream(),
                (Stream<Annotation>) (Stream<?>) numerals.stream(),
                (Stream<Annotation>) (Stream<?>) errata.stream(),
                (Stream<Annotation>) (Stream<?>) drawings.stream(),
                (Stream<Annotation>) (Stream<?>) calculations.stream(),
                (Stream<Annotation>) (Stream<?>) graphs.stream(),
                (Stream<Annotation>) (Stream<?>) tables.stream(),
                (Stream<Annotation>) (Stream<?>) links.stream()
        ).flatMap(s -> s);
    }

    /**
     * Finds an annotation by its ID.
     *
     * @param id the annotation ID to find
     * @return the matching annotation, or null if not found
     */
    public Annotation getAnnotation(String id) {
        return stream().filter(a -> a.getId() != null && a.getId().equals(id)).findFirst().orElse(null);
    }

    /**
     * Returns all annotations on this page as a list.
     *
     * @return a list of all annotations
     */
    public List<Annotation> getAnnotations() {
        return stream().toList();
    }

    /**
     * Returns the page identifier.
     *
     * @return the ID
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the page identifier.
     *
     * @param id the ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the page filename.
     *
     * @return the page filename
     */
    public String getPage() {
        return page;
    }

    /**
     * Sets the page filename.
     *
     * @param page the page filename
     */
    public void setPage(String page) {
        this.page = page;
    }

    /**
     * Returns the reader who made annotations on this page.
     *
     * @return the reader name
     */
    public String getReader() {
        return reader;
    }

    /**
     * Sets the reader name.
     *
     * @param reader the reader name
     */
    public void setReader(String reader) {
        this.reader = reader;
    }

    /**
     * Returns the pagination label for this page.
     *
     * @return the pagination label, or null
     */
    public String getPagination() {
        return pagination;
    }

    /**
     * Sets the pagination label.
     *
     * @param pagination the pagination label
     */
    public void setPagination(String pagination) {
        this.pagination = pagination;
    }

    /**
     * Returns the page signature.
     *
     * @return the signature, or null
     */
    public String getSignature() {
        return signature;
    }

    /**
     * Sets the page signature.
     *
     * @param signature the signature
     */
    public void setSignature(String signature) {
        this.signature = signature;
    }

    /**
     * Returns the origin of this page's annotations.
     *
     * @return the origin, or null
     */
    public String getOrigin() {
        return origin;
    }

    /**
     * Sets the origin.
     *
     * @param origin the origin
     */
    public void setOrigin(String origin) {
        this.origin = origin;
    }

    /**
     * Returns the transcription status.
     *
     * @return the status, or null
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status.
     *
     * @param status the status
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Returns the host page identifier (for inserts).
     *
     * @return the host page, or null
     */
    public String getHostPage() {
        return hostPage;
    }

    /**
     * Sets the host page.
     *
     * @param hostPage the host page identifier
     */
    public void setHostPage(String hostPage) {
        this.hostPage = hostPage;
    }

    /**
     * Returns the related page identifier.
     *
     * @return the related page, or null
     */
    public String getRelatedPage() {
        return relatedPage;
    }

    /**
     * Sets the related page.
     *
     * @param relatedPage the related page identifier
     */
    public void setRelatedPage(String relatedPage) {
        this.relatedPage = relatedPage;
    }

    /**
     * Returns the marginalia annotations on this page.
     *
     * @return the marginalia list (never null)
     */
    public List<Marginalia> getMarginalia() {
        return marginalia;
    }

    /**
     * Sets the marginalia list.
     *
     * @param marginalia the marginalia
     */
    public void setMarginalia(List<Marginalia> marginalia) {
        this.marginalia = marginalia;
    }

    /**
     * Returns the mark annotations on this page.
     *
     * @return the marks list (never null)
     */
    public List<Mark> getMarks() {
        return marks;
    }

    /**
     * Sets the marks list.
     *
     * @param marks the marks
     */
    public void setMarks(List<Mark> marks) {
        this.marks = marks;
    }

    /**
     * Returns the symbol annotations on this page.
     *
     * @return the symbols list (never null)
     */
    public List<Symbol> getSymbols() {
        return symbols;
    }

    /**
     * Sets the symbols list.
     *
     * @param symbols the symbols
     */
    public void setSymbols(List<Symbol> symbols) {
        this.symbols = symbols;
    }

    /**
     * Returns the underline annotations on this page.
     *
     * @return the underlines list (never null)
     */
    public List<Underline> getUnderlines() {
        return underlines;
    }

    /**
     * Sets the underlines list.
     *
     * @param underlines the underlines
     */
    public void setUnderlines(List<Underline> underlines) {
        this.underlines = underlines;
    }

    /**
     * Returns the numeral annotations on this page.
     *
     * @return the numerals list (never null)
     */
    public List<Numeral> getNumerals() {
        return numerals;
    }

    /**
     * Sets the numerals list.
     *
     * @param numerals the numerals
     */
    public void setNumerals(List<Numeral> numerals) {
        this.numerals = numerals;
    }

    /**
     * Returns the errata annotations on this page.
     *
     * @return the errata list (never null)
     */
    public List<Errata> getErrata() {
        return errata;
    }

    /**
     * Sets the errata list.
     *
     * @param errata the errata
     */
    public void setErrata(List<Errata> errata) {
        this.errata = errata;
    }

    /**
     * Returns the drawing annotations on this page.
     *
     * @return the drawings list (never null)
     */
    public List<Drawing> getDrawings() {
        return drawings;
    }

    /**
     * Sets the drawings list.
     *
     * @param drawings the drawings
     */
    public void setDrawings(List<Drawing> drawings) {
        this.drawings = drawings;
    }

    /**
     * Returns the calculation annotations on this page.
     *
     * @return the calculations list (never null)
     */
    public List<Calculation> getCalculations() {
        return calculations;
    }

    /**
     * Sets the calculations list.
     *
     * @param calculations the calculations
     */
    public void setCalculations(List<Calculation> calculations) {
        this.calculations = calculations;
    }

    /**
     * Returns the graph annotations on this page.
     *
     * @return the graphs list (never null)
     */
    public List<Graph> getGraphs() {
        return graphs;
    }

    /**
     * Sets the graphs list.
     *
     * @param graphs the graphs
     */
    public void setGraphs(List<Graph> graphs) {
        this.graphs = graphs;
    }

    /**
     * Returns the table annotations on this page.
     *
     * @return the tables list (never null)
     */
    public List<Table> getTables() {
        return tables;
    }

    /**
     * Sets the tables list.
     *
     * @param tables the tables
     */
    public void setTables(List<Table> tables) {
        this.tables = tables;
    }

    /**
     * Returns the physical link annotations on this page.
     *
     * @return the links list (never null)
     */
    public List<PhysicalLink> getLinks() {
        return links;
    }

    /**
     * Sets the links list.
     *
     * @param links the links
     */
    public void setLinks(List<PhysicalLink> links) {
        this.links = links;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AnnotatedPage that)) return false;
        return Objects.equals(id, that.id) &&
                Objects.equals(page, that.page) &&
                Objects.equals(reader, that.reader) &&
                Objects.equals(pagination, that.pagination) &&
                Objects.equals(signature, that.signature) &&
                Objects.equals(origin, that.origin) &&
                Objects.equals(status, that.status) &&
                Objects.equals(hostPage, that.hostPage) &&
                Objects.equals(relatedPage, that.relatedPage) &&
                Objects.equals(marginalia, that.marginalia) &&
                Objects.equals(marks, that.marks) &&
                Objects.equals(symbols, that.symbols) &&
                Objects.equals(underlines, that.underlines) &&
                Objects.equals(numerals, that.numerals) &&
                Objects.equals(errata, that.errata) &&
                Objects.equals(drawings, that.drawings) &&
                Objects.equals(calculations, that.calculations) &&
                Objects.equals(graphs, that.graphs) &&
                Objects.equals(tables, that.tables) &&
                Objects.equals(links, that.links);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, page, reader, pagination, signature, origin, status,
                hostPage, relatedPage, marginalia, marks, symbols, underlines,
                numerals, errata, drawings, calculations, graphs, tables, links);
    }

    @Override
    public String toString() {
        return "AnnotatedPage{" +
                "id='" + id + '\'' +
                ", page='" + page + '\'' +
                ", reader='" + reader + '\'' +
                ", pagination='" + pagination + '\'' +
                ", signature='" + signature + '\'' +
                ", marginalia=" + marginalia.size() +
                ", marks=" + marks.size() +
                ", symbols=" + symbols.size() +
                ", underlines=" + underlines.size() +
                ", numerals=" + numerals.size() +
                ", errata=" + errata.size() +
                ", drawings=" + drawings.size() +
                ", calculations=" + calculations.size() +
                ", graphs=" + graphs.size() +
                ", tables=" + tables.size() +
                ", links=" + links.size() +
                '}';
    }
}
