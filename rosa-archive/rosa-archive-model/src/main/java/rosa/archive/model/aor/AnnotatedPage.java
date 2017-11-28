package rosa.archive.model.aor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import rosa.archive.model.HasId;

/**
 *
 */
public class AnnotatedPage implements HasId, Serializable {
    private static final long serialVersionUID = 1L;

    // TODO Just keep list of annotations and filter by type as needed?
    
    private String id;
    private String page;
    private String reader;
    private String pagination;
    private String signature;
    private List<Marginalia> marginalia;
    private List<Mark> marks;
    private List<Symbol> symbols;
    private List<Underline> underlines;
    private List<Numeral> numerals;
    private List<Substitution> subs;
    private List<Errata> errata;
    private List<Drawing> drawings;
    private List<Reference> refs;

    public AnnotatedPage() {
        marginalia = new ArrayList<>();
        marks = new ArrayList<>();
        symbols = new ArrayList<>();
        underlines = new ArrayList<>();
        numerals = new ArrayList<>();
        subs = new ArrayList<>();
        errata = new ArrayList<>();
        drawings = new ArrayList<>();
        refs = new ArrayList<>();
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
//    public Stream<? extends Annotation> stream() {
//    	return Stream.of(marginalia.stream(), marks.stream(), symbols.stream(), underlines.stream(), numerals.stream(),
//    				errata.stream(), drawings.stream()).reduce(Stream::concat).orElseGet(Stream::empty);
//    }
//    
//    public Annotation getAnnotation(String id) {
//    	return stream().filter(a -> a.getId().equals(id)).findFirst().get();
//    }

    public List<Annotation> getAnnotations() {
    	List<Annotation> result = new ArrayList<>();
    	
    	result.addAll(marginalia);
    	result.addAll(marks);
    	result.addAll(symbols);
    	result.addAll(underlines);
    	result.addAll(numerals);
    	result.addAll(subs);
    	result.addAll(errata);
    	result.addAll(drawings);
    	result.addAll(refs);
    	
    	return result;
    }
 
    public Annotation getAnnotation(String id) {
    	for (Annotation a: getAnnotations()) {
    		if (a.getId().equals(id)) {
    			return a;
    		}
    	}
    	
    	return null;
	}

    @SuppressWarnings("unchecked")
	public <T extends Annotation> T getAnnotation(String id, Class<T> type) {
    	return type.cast(getAnnotation(id));
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

    public List<Substitution> getSubs() {
        return subs;
    }

    public void setSubs(List<Substitution> subs) {
        this.subs = subs;
    }

    public List<Errata> getErrata() {
        return errata;
    }

    public void setErrata(List<Errata> errata) {
        this.errata = errata;
    }

    public List<Reference> getRefs() {
        return refs;
    }

    public void setRefs(List<Reference> refs) {
        this.refs = refs;
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
        if (marginalia != null ? !marginalia.equals(that.marginalia) : that.marginalia != null) return false;
        if (marks != null ? !marks.equals(that.marks) : that.marks != null) return false;
        if (symbols != null ? !symbols.equals(that.symbols) : that.symbols != null) return false;
        if (underlines != null ? !underlines.equals(that.underlines) : that.underlines != null) return false;
        if (numerals != null ? !numerals.equals(that.numerals) : that.numerals != null) return false;
        if (subs != null ? !subs.equals(that.subs) : that.subs != null) return false;
        if (errata != null ? !errata.equals(that.errata) : that.errata != null) return false;
        if (drawings != null ? !drawings.equals(that.drawings) : that.drawings != null) return false;
        return refs != null ? refs.equals(that.refs) : that.refs == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (page != null ? page.hashCode() : 0);
        result = 31 * result + (reader != null ? reader.hashCode() : 0);
        result = 31 * result + (pagination != null ? pagination.hashCode() : 0);
        result = 31 * result + (signature != null ? signature.hashCode() : 0);
        result = 31 * result + (marginalia != null ? marginalia.hashCode() : 0);
        result = 31 * result + (marks != null ? marks.hashCode() : 0);
        result = 31 * result + (symbols != null ? symbols.hashCode() : 0);
        result = 31 * result + (underlines != null ? underlines.hashCode() : 0);
        result = 31 * result + (numerals != null ? numerals.hashCode() : 0);
        result = 31 * result + (subs != null ? subs.hashCode() : 0);
        result = 31 * result + (errata != null ? errata.hashCode() : 0);
        result = 31 * result + (drawings != null ? drawings.hashCode() : 0);
        result = 31 * result + (refs != null ? refs.hashCode() : 0);
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
                ", marginalia=" + marginalia +
                ", marks=" + marks +
                ", symbols=" + symbols +
                ", underlines=" + underlines +
                ", numerals=" + numerals +
                ", subs=" + subs +
                ", errata=" + errata +
                ", drawings=" + drawings +
                ", refs=" + refs +
                '}';
    }
}
