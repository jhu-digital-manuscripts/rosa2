package rosa.archive.model.aor;

import rosa.archive.model.HasId;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class AnnotatedPage implements HasId, Serializable {
    private static final long serialVersionUID = 1L;

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
    private List<Errata> errata;
    private List<Drawing> drawings;

    public AnnotatedPage() {
        marginalia = new ArrayList<>();
        marks = new ArrayList<>();
        symbols = new ArrayList<>();
        underlines = new ArrayList<>();
        numerals = new ArrayList<>();
        errata = new ArrayList<>();
        drawings = new ArrayList<>();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public Annotation getAnnotation(String id) {
        List<Class<? extends Annotation>> types =
                Arrays.asList(Marginalia.class, Mark.class, Symbol.class, Underline.class,
                        Numeral.class, Drawing.class, Errata.class);

        for (Class c : types) {
            Annotation a = getAnnotation(id, c);
            if (a != null) {
                return a;
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends Annotation> T getAnnotation(String id, Class<T> type) {
        Annotation ann = null;
        if (type.equals(Marginalia.class)) {
            ann = getAnnotation(id, getMarginalia());
        } else if (type.equals(Mark.class)) {
            ann = getAnnotation(id, getMarks());
        } else if (type.equals(Symbol.class)) {
            ann = getAnnotation(id, getSymbols());
        } else if (type.equals(Underline.class)) {
            ann = getAnnotation(id, getUnderlines());
        } else if (type.equals(Numeral.class)) {
            ann = getAnnotation(id, getNumerals());
        } else if (type.equals(Drawing.class)) {
            ann = getAnnotation(id, getDrawings());
        } else if (type.equals(Errata.class)) {
            ann = getAnnotation(id, getErrata());
        }

        if (ann != null) {
            return (T) ann;
        } else {
            return null;
        }
    }

    private Annotation getAnnotation(String id, List<? extends Annotation> annotations) {
        for (Annotation ann : annotations) {
            if (ann.getId().equals(id)) {
                return ann;
            }
        }

        return null;
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

    public List<Errata> getErrata() {
        return errata;
    }

    public void setErrata(List<Errata> errata) {
        this.errata = errata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AnnotatedPage that = (AnnotatedPage) o;

        if (errata != null ? !errata.equals(that.errata) : that.errata != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (marginalia != null ? !marginalia.equals(that.marginalia) : that.marginalia != null) return false;
        if (marks != null ? !marks.equals(that.marks) : that.marks != null) return false;
        if (numerals != null ? !numerals.equals(that.numerals) : that.numerals != null) return false;
        if (page != null ? !page.equals(that.page) : that.page != null) return false;
        if (pagination != null ? !pagination.equals(that.pagination) : that.pagination != null) return false;
        if (reader != null ? !reader.equals(that.reader) : that.reader != null) return false;
        if (signature != null ? !signature.equals(that.signature) : that.signature != null) return false;
        if (symbols != null ? !symbols.equals(that.symbols) : that.symbols != null) return false;
        if (underlines != null ? !underlines.equals(that.underlines) : that.underlines != null) return false;
        if (drawings != null ? !drawings.equals(that.drawings) : that.drawings != null) return false;

        return true;
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
        result = 31 * result + (errata != null ? errata.hashCode() : 0);
        result = 31 * result + (drawings != null ? drawings.hashCode() : 0);
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
                ", errata=" + errata +
                ", drawings=" + drawings +
                '}';
    }
}
