package rosa.archive.model.aor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import rosa.archive.model.HasId;

/**
 * This object aggregates information from the &lt;page&gt; element as well as the &lt;annotation&gt;
 * element.
 *
 * &lt;page filename pagination signature reader origin status hostPage relatedPage /&gt;
 *
 * Attributes:
 * <ul>
 *   <li>filename (required) : page ID</li>
 *   <li>pagination (optional)</li>
 *   <li>signature (optional)</li>
 *   <li>reader (required) name of reader that made annotations on the page</li>
 *   <li>origin (optional)</li>
 *   <li>status (optional)</li>
 *   <li>host_page (optional) : different page that may contain this page, useful for inserts</li>
 *   <li>related_page (optional): similar to 'hostPage' ??</li>
 * </ul>
 *
 * 'page' element contains no elements.
 *
 * &lt;annotation&gt;
 *
 * This element has no attributes, but contains the different types of annotations:
 * <ul>
 *   <li>marginalia (zero or more) : {@link Marginalia}</li>
 *   <li>underline (zero or more) : {@link Underline}</li>
 *   <li>symbol (zero or more) : {@link Symbol}</li>
 *   <li>mark (zero or more) : {@link Mark}</li>
 *   <li>numeral (zero or more) : {@link Numeral}</li>
 *   <li>errata (zero or more) : {@link Errata}</li>
 *   <li>drawing (zero or more) : {@link Drawing}</li>
 *   <li>calculation (zero or more) : {@link Calculation}</li>
 *   <li>graph (zero or more) : {@link Graph}</li>
 *   <li>table (zero or more) : {@link Table}</li>
 *   <li>physical_link (zero or more) : {@link PhysicalLink}</li>
 * </ul>
 */
public class AnnotatedPage implements HasId, Serializable {
    private static final long serialVersionUID = 1L;

    // TODO Just keep list of annotations and filter by type as needed?
    
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

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    // TODO Cannot have stream because no support in GWT yet
    public Stream<? extends Annotation> stream() {
    	return Stream.of(marginalia.stream(), marks.stream(), symbols.stream(), underlines.stream(), numerals.stream(),
    				errata.stream(), drawings.stream(), calculations.stream(), graphs.stream(), tables.stream(),
                    links.stream())
                .reduce(Stream::concat)
                .orElseGet(Stream::empty);
    }

    // Do we have a guarantee that ID is unique?
    public Annotation getAnnotation(String id) {
    	return stream().filter(a -> a.getId().equals(id)).findFirst().orElse(null);
    }

    public List<Annotation> getAnnotations() {
        return stream().collect(Collectors.toList());
    }
    
    // TODO Cannot use type.cast because of lack of GWT support
    
    @SuppressWarnings("unchecked")
	public <T extends Annotation> T getAnnotation(String id, Class<T> type) {
    	return (T) getAnnotation(id);
    }

	public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public String getReader() {
        return reader;
    }

    public void setReader(String reader) {
        this.reader = reader;
    }

    public String getPagination() {
        return pagination;
    }

    public void setPagination(String pagination) {
        this.pagination = pagination;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHostPage() {
        return hostPage;
    }

    public void setHostPage(String hostPage) {
        this.hostPage = hostPage;
    }

    public String getRelatedPage() {
        return relatedPage;
    }

    public void setRelatedPage(String relatedPage) {
        this.relatedPage = relatedPage;
    }

    public List<Marginalia> getMarginalia() {
        return marginalia;
    }

    public void setMarginalia(List<Marginalia> marginalia) {
        this.marginalia = marginalia;
    }

    public List<Mark> getMarks() {
        return marks;
    }

    public void setMarks(List<Mark> marks) {
        this.marks = marks;
    }

    public List<Symbol> getSymbols() {
        return symbols;
    }

    public void setSymbols(List<Symbol> symbols) {
        this.symbols = symbols;
    }

    public List<Underline> getUnderlines() {
        return underlines;
    }

    public void setUnderlines(List<Underline> underlines) {
        this.underlines = underlines;
    }

    public List<Numeral> getNumerals() {
        return numerals;
    }

    public void setNumerals(List<Numeral> numerals) {
        this.numerals = numerals;
    }

    public List<Drawing> getDrawings() {
        return drawings;
    }

    public void setDrawings(List<Drawing> drawings) {
        this.drawings = drawings;
    }

    public List<Errata> getErrata() {
        return errata;
    }

    public void setErrata(List<Errata> errata) {
        this.errata = errata;
    }

    public List<Calculation> getCalculations() {
        return calculations;
    }

    public void setCalculations(List<Calculation> calculations) {
        this.calculations = calculations;
    }

    public List<Graph> getGraphs() {
        return graphs;
    }

    public void setGraphs(List<Graph> graphs) {
        this.graphs = graphs;
    }

    public List<Table> getTables() {
        return tables;
    }

    public void setTables(List<Table> tables) {
        this.tables = tables;
    }

    public List<PhysicalLink> getLinks() {
        return links;
    }

    public void setLinks(List<PhysicalLink> links) {
        this.links = links;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AnnotatedPage that = (AnnotatedPage) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (page != null ? !page.equals(that.page) : that.page != null) return false;
        if (reader != null ? !reader.equals(that.reader) : that.reader != null) return false;
        if (pagination != null ? !pagination.equals(that.pagination) : that.pagination != null) return false;
        if (signature != null ? !signature.equals(that.signature) : that.signature != null) return false;
        if (origin != null ? !origin.equals(that.origin) : that.origin != null) return false;
        if (status != null ? !status.equals(that.status) : that.status != null) return false;
        if (hostPage != null ? !hostPage.equals(that.hostPage) : that.hostPage != null) return false;
        if (relatedPage != null ? !relatedPage.equals(that.relatedPage) : that.relatedPage != null) return false;
        if (marginalia != null ? !marginalia.equals(that.marginalia) : that.marginalia != null) return false;
        if (marks != null ? !marks.equals(that.marks) : that.marks != null) return false;
        if (symbols != null ? !symbols.equals(that.symbols) : that.symbols != null) return false;
        if (underlines != null ? !underlines.equals(that.underlines) : that.underlines != null) return false;
        if (numerals != null ? !numerals.equals(that.numerals) : that.numerals != null) return false;
        if (errata != null ? !errata.equals(that.errata) : that.errata != null) return false;
        if (drawings != null ? !drawings.equals(that.drawings) : that.drawings != null) return false;
        if (calculations != null ? !calculations.equals(that.calculations) : that.calculations != null) return false;
        if (graphs != null ? !graphs.equals(that.graphs) : that.graphs != null) return false;
        if (tables != null ? !tables.equals(that.tables) : that.tables != null) return false;
        return links != null ? links.equals(that.links) : that.links == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (page != null ? page.hashCode() : 0);
        result = 31 * result + (reader != null ? reader.hashCode() : 0);
        result = 31 * result + (pagination != null ? pagination.hashCode() : 0);
        result = 31 * result + (signature != null ? signature.hashCode() : 0);
        result = 31 * result + (origin != null ? origin.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (hostPage != null ? hostPage.hashCode() : 0);
        result = 31 * result + (relatedPage != null ? relatedPage.hashCode() : 0);
        result = 31 * result + (marginalia != null ? marginalia.hashCode() : 0);
        result = 31 * result + (marks != null ? marks.hashCode() : 0);
        result = 31 * result + (symbols != null ? symbols.hashCode() : 0);
        result = 31 * result + (underlines != null ? underlines.hashCode() : 0);
        result = 31 * result + (numerals != null ? numerals.hashCode() : 0);
        result = 31 * result + (errata != null ? errata.hashCode() : 0);
        result = 31 * result + (drawings != null ? drawings.hashCode() : 0);
        result = 31 * result + (calculations != null ? calculations.hashCode() : 0);
        result = 31 * result + (graphs != null ? graphs.hashCode() : 0);
        result = 31 * result + (tables != null ? tables.hashCode() : 0);
        result = 31 * result + (links != null ? links.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "AnnotatedPage{" +
                "id='" + id + '\'' +
                ", page='" + page + '\'' +
                ", reader='" + reader + '\'' +
                ", pagination='" + pagination + '\'' +
                ", signature='" + signature + '\'' +
                ", origin='" + origin + '\'' +
                ", status='" + status + '\'' +
                ", hostPage='" + hostPage + '\'' +
                ", relatedPage='" + relatedPage + '\'' +
                ", marginalia:" + marginalia.size() +
                ", marks:" + marks.size() +
                ", symbols:" + symbols.size() +
                ", underlines:" + underlines.size() +
                ", numerals:" + numerals.size() +
                ", errata:" + errata.size() +
                ", drawings:" + drawings.size() +
                ", calculations:" + calculations.size() +
                ", graphs:" + graphs.size() +
                ", tables:" + tables.size() +
                ", links:" + links.size() +
                '}';
    }
}
