package rosa.archive.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Describes a text contained within a book, including its physical extent (first/last page)
 * and structural properties (columns, lines, gatherings).
 */
public final class BookText {

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
     * Creates an empty BookText with default values (-1 for numeric fields).
     */
    public BookText() {
        linesPerColumn = -1;
        columnsPerPage = -1;
        leavesPerGathering = -1;
        numberOfIllustrations = -1;
        numberOfPages = -1;
        authors = new ArrayList<>();
    }

    /**
     * Returns the number of lines per column.
     *
     * @return lines per column, or -1 if unknown
     */
    public int getLinesPerColumn() {
        return linesPerColumn;
    }

    /**
     * Sets the number of lines per column.
     *
     * @param linesPerColumn lines per column
     */
    public void setLinesPerColumn(int linesPerColumn) {
        this.linesPerColumn = linesPerColumn;
    }

    /**
     * Returns the number of columns per page.
     *
     * @return columns per page, or -1 if unknown
     */
    public int getColumnsPerPage() {
        return columnsPerPage;
    }

    /**
     * Sets the number of columns per page.
     *
     * @param columnsPerPage columns per page
     */
    public void setColumnsPerPage(int columnsPerPage) {
        this.columnsPerPage = columnsPerPage;
    }

    /**
     * Returns the number of leaves per gathering.
     *
     * @return leaves per gathering, or -1 if unknown
     */
    public int getLeavesPerGathering() {
        return leavesPerGathering;
    }

    /**
     * Sets the number of leaves per gathering.
     *
     * @param leavesPerGathering leaves per gathering
     */
    public void setLeavesPerGathering(int leavesPerGathering) {
        this.leavesPerGathering = leavesPerGathering;
    }

    /**
     * Returns the number of illustrations in this text.
     *
     * @return number of illustrations, or -1 if unknown
     */
    public int getNumberOfIllustrations() {
        return numberOfIllustrations;
    }

    /**
     * Sets the number of illustrations in this text.
     *
     * @param numberOfIllustrations number of illustrations
     */
    public void setNumberOfIllustrations(int numberOfIllustrations) {
        this.numberOfIllustrations = numberOfIllustrations;
    }

    /**
     * Returns the number of pages spanned by this text.
     *
     * @return number of pages, or -1 if unknown
     */
    public int getNumberOfPages() {
        return numberOfPages;
    }

    /**
     * Sets the number of pages spanned by this text.
     *
     * @param numberOfPages number of pages
     */
    public void setNumberOfPages(int numberOfPages) {
        this.numberOfPages = numberOfPages;
    }

    /**
     * Returns the title of this text.
     *
     * @return the title, or {@code null} if not set
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title of this text.
     *
     * @param title the title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the identifier of the first page of this text.
     *
     * @return the first page identifier, or {@code null} if not set
     */
    public String getFirstPage() {
        return firstPage;
    }

    /**
     * Sets the identifier of the first page of this text.
     *
     * @param firstPage the first page identifier
     */
    public void setFirstPage(String firstPage) {
        this.firstPage = firstPage;
    }

    /**
     * Returns the identifier of the last page of this text.
     *
     * @return the last page identifier, or {@code null} if not set
     */
    public String getLastPage() {
        return lastPage;
    }

    /**
     * Sets the identifier of the last page of this text.
     *
     * @param lastPage the last page identifier
     */
    public void setLastPage(String lastPage) {
        this.lastPage = lastPage;
    }

    /**
     * Returns the language code of this text.
     *
     * @return the language code, or {@code null} if not set
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Sets the language code of this text.
     *
     * @param language the language code
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Returns the list of authors of this text.
     *
     * @return the authors list
     */
    public List<String> getAuthors() {
        return authors;
    }

    /**
     * Adds an author to this text.
     *
     * @param author the author name to add
     */
    public void addAuthor(String author) {
        authors.add(author);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookText that)) return false;
        return linesPerColumn == that.linesPerColumn &&
                columnsPerPage == that.columnsPerPage &&
                leavesPerGathering == that.leavesPerGathering &&
                numberOfIllustrations == that.numberOfIllustrations &&
                numberOfPages == that.numberOfPages &&
                Objects.equals(title, that.title) &&
                Objects.equals(firstPage, that.firstPage) &&
                Objects.equals(lastPage, that.lastPage) &&
                Objects.equals(language, that.language) &&
                Objects.equals(authors, that.authors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(linesPerColumn, columnsPerPage, leavesPerGathering,
                numberOfIllustrations, numberOfPages, title, firstPage, lastPage,
                language, authors);
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
                ", authors=" + authors +
                '}';
    }
}
