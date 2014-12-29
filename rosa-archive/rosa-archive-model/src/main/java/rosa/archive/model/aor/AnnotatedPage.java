package rosa.archive.model.aor;

import rosa.archive.model.HasId;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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

    public AnnotatedPage() {
        marginalia = new ArrayList<>();
        marks = new ArrayList<>();
        symbols = new ArrayList<>();
        underlines = new ArrayList<>();
        numerals = new ArrayList<>();
        errata = new ArrayList<>();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
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

        AnnotatedPage page1 = (AnnotatedPage) o;

        if (errata != null ? !errata.equals(page1.errata) : page1.errata != null) return false;
        if (id != null ? !id.equals(page1.id) : page1.id != null) return false;
        if (marginalia != null ? !marginalia.equals(page1.marginalia) : page1.marginalia != null) return false;
        if (marks != null ? !marks.equals(page1.marks) : page1.marks != null) return false;
        if (numerals != null ? !numerals.equals(page1.numerals) : page1.numerals != null) return false;
        if (page != null ? !page.equals(page1.page) : page1.page != null) return false;
        if (pagination != null ? !pagination.equals(page1.pagination) : page1.pagination != null) return false;
        if (reader != null ? !reader.equals(page1.reader) : page1.reader != null) return false;
        if (signature != null ? !signature.equals(page1.signature) : page1.signature != null) return false;
        if (symbols != null ? !symbols.equals(page1.symbols) : page1.symbols != null) return false;
        if (underlines != null ? !underlines.equals(page1.underlines) : page1.underlines != null) return false;

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
                '}';
    }
}
