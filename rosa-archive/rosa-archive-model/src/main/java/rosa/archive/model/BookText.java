package rosa.archive.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public final class BookText implements Serializable {
    private static final long serialVersionUID = 1L;

    private int linesPerColumn;
    private int columnsPerPage;
    private int leavesPerGathering;
    private int numberOfIllustrations;
    private int numberOfPages;
    private String title;
    private String firstPage;
    private String lastPage;

    private String language;
    private List<String> authors;

    /**
     * Create empty BookText
     */
    public BookText() {
        linesPerColumn = -1;
        columnsPerPage = -1;
        leavesPerGathering = -1;
        numberOfIllustrations = -1;
        numberOfPages = -1;
        authors = new ArrayList<>();
    }

    public int getLinesPerColumn() {
        return linesPerColumn;
    }

    public void setLinesPerColumn(int linesPerColumn) {
        this.linesPerColumn = linesPerColumn;
    }

    public int getColumnsPerPage() {
        return columnsPerPage;
    }

    public void setColumnsPerPage(int columnsPerPage) {
        this.columnsPerPage = columnsPerPage;
    }

    public int getLeavesPerGathering() {
        return leavesPerGathering;
    }

    public void setLeavesPerGathering(int leavesPerGathering) {
        this.leavesPerGathering = leavesPerGathering;
    }

    public int getNumberOfIllustrations() {
        return numberOfIllustrations;
    }

    public void setNumberOfIllustrations(int numberOfIllustrations) {
        this.numberOfIllustrations = numberOfIllustrations;
    }

    public int getNumberOfPages() {
        return numberOfPages;
    }

    public void setNumberOfPages(int numberOfPages) {
        this.numberOfPages = numberOfPages;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFirstPage() {
        return firstPage;
    }

    public void setFirstPage(String firstPage) {
        this.firstPage = firstPage;
    }

    public String getLastPage() {
        return lastPage;
    }

    public void setLastPage(String lastPage) {
        this.lastPage = lastPage;
    }
    
    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public void addAuthor(String author) {
        authors.add(author);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookText)) return false;

        BookText text = (BookText) o;

        if (linesPerColumn != text.linesPerColumn) return false;
        if (columnsPerPage != text.columnsPerPage) return false;
        if (leavesPerGathering != text.leavesPerGathering) return false;
        if (numberOfIllustrations != text.numberOfIllustrations) return false;
        if (numberOfPages != text.numberOfPages) return false;
        if (title != null ? !title.equals(text.title) : text.title != null) return false;
        if (firstPage != null ? !firstPage.equals(text.firstPage) : text.firstPage != null) return false;
        if (lastPage != null ? !lastPage.equals(text.lastPage) : text.lastPage != null) return false;
        if (authors != null ? !authors.equals(text.authors) : text.authors != null) return false;
        return !(language != null ? !language.equals(text.language) : text.language != null);

    }

    @Override
    public int hashCode() {
        int result = linesPerColumn;
        result = 31 * result + columnsPerPage;
        result = 31 * result + leavesPerGathering;
        result = 31 * result + numberOfIllustrations;
        result = 31 * result + numberOfPages;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (firstPage != null ? firstPage.hashCode() : 0);
        result = 31 * result + (lastPage != null ? lastPage.hashCode() : 0);
        result = 31 * result + (language != null ? language.hashCode() : 0);
        result = 31 * result + (authors != null ? authors.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BookText{" +
                "linesPerColumn=" + linesPerColumn +
                ", columnsPerPage=" + columnsPerPage +
                ", leavesPerGathering=" + leavesPerGathering +
                ", numberOfIllustrations=" + numberOfIllustrations +
                ", numberOfPages=" + numberOfPages +
                ", title='" + title + '\'' +
                ", firstPage='" + firstPage + '\'' +
                ", lastPage='" + lastPage + '\'' +
                ", language='" + language + '\'' +
                ", authors='" + authors + '\'' +
                '}';
    }
}
